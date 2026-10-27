package com.cmpe172.game_coaching_booking.notification;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.cmpe172.game_coaching_booking.observability.SchedulerMetrics;

/** Tests the real HTTP client, not a mocked RestClient. */
class HttpNotificationGatewayTest {
    private HttpServer server;
    private ExecutorService executor;
    private final SchedulerMetrics metrics = new SchedulerMetrics();
    private final NotificationRequest request = new NotificationRequest(
            "appointment-10-booking", "one@example.com", NotificationType.BOOKING_CONFIRMATION, "Confirmed");

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();
    }
    @AfterEach
    void stop() { server.stop(0); executor.shutdownNow(); }

    @Test
    void transientFailuresRetryIdenticalPayloadThenSucceed() {
        AtomicInteger attempts = new AtomicInteger();
        List<String> bodies = new CopyOnWriteArrayList<>();
        server.createContext("/internal/notifications", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            assertEquals("test-token", exchange.getRequestHeaders().getFirst("X-Notification-Token"));
            exchange.sendResponseHeaders(attempts.incrementAndGet() < 3 ? 503 : 200, -1);
            exchange.close();
        });
        assertTrue(gateway(500, 3).send(request));
        assertEquals(3, attempts.get());
        assertEquals(1, bodies.stream().distinct().count());
        assertTrue(bodies.get(0).contains("appointment-10-booking"));
    }

    @Test
    void slowServiceTimesOutAndReturnsFallbackWithinBoundedAttempts() {
        server.createContext("/internal/notifications", exchange -> {
            try {
                Thread.sleep(1000);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (IOException disconnected) {
                // Expected: the client has already timed out.
            } finally { exchange.close(); }
        });
        long started = System.nanoTime();
        assertFalse(gateway(50, 2).send(request));
        assertTrue((System.nanoTime() - started) / 1_000_000 < 3000);
        assertEquals(2L, metrics.snapshot().get("notification_http_attempts_total"));
    }

    @Test
    void permanentClientErrorDoesNotWasteAllRetries() {
        AtomicInteger attempts = new AtomicInteger();
        server.createContext("/internal/notifications", exchange -> {
            attempts.incrementAndGet();
            exchange.sendResponseHeaders(403, -1);
            exchange.close();
        });
        assertFalse(gateway(500, 3).send(request));
        assertEquals(1, attempts.get());
    }

    @Test
    void rateLimitIsRetried() {
        AtomicInteger attempts = new AtomicInteger();
        server.createContext("/internal/notifications", exchange -> {
            exchange.sendResponseHeaders(attempts.incrementAndGet() == 1 ? 429 : 200, -1);
            exchange.close();
        });
        assertTrue(gateway(500, 3).send(request));
        assertEquals(2, attempts.get());
    }

    private HttpNotificationGateway gateway(int readTimeout, int attempts) {
        return new HttpNotificationGateway(new NotificationProperties(
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-token", 100, readTimeout, attempts, 0), metrics);
    }
}
