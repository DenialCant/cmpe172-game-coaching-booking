package com.cmpe172.game_coaching_booking;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.cmpe172.game_coaching_booking.notification.*;
import com.cmpe172.game_coaching_booking.observability.SchedulerMetrics;
import com.cmpe172.game_coaching_booking.service.BookingService;
import com.cmpe172.game_coaching_booking.service.BookingTransactionService;
import com.cmpe172.game_coaching_booking.repository.AppointmentRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationHttpIntegrationTest {
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired NotificationOutboxRepository outbox;
    @Autowired BookingTransactionService transactions;
    @Autowired AppointmentRepository appointments;
    @Autowired SchedulerMetrics metrics;
    private final HttpClient http = HttpClient.newHttpClient();
    private static final String JSON = """
            {"notificationId":"test-123","recipient":"one@example.com",
             "type":"BOOKING_CONFIRMATION","message":"Confirmed"}
            """;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM mock_notification_receipts");
        jdbc.update("DELETE FROM notification_outbox");
        jdbc.update("DELETE FROM appointments");
    }

    @Test
    void duplicateDeliveryAndLostResponseReplayHaveOneStoredEffect() throws Exception {
        assertEquals(200, post(JSON, "local-demo-token").statusCode());
        assertTrue(post(JSON, "local-demo-token").body().contains("DUPLICATE"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mock_notification_receipts", Integer.class));
        assertEquals(409, post(JSON.replace("Confirmed", "Changed"), "local-demo-token").statusCode());
    }

    @Test
    void invalidPayloadAndUnauthorizedCallsAreRejected() throws Exception {
        assertEquals(403, post(JSON, "wrong-token").statusCode());
        assertEquals(400, post(JSON.replace("one@example.com", "not-an-email"), "local-demo-token").statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mock_notification_receipts", Integer.class));
    }

    @Test
    void commitPrecedesHttpAndDurableOutboxRecoveryWorks() {
        var actualGateway = new HttpNotificationGateway(new NotificationProperties(
                "http://localhost:" + port, "local-demo-token", 500, 1000, 3, 0), metrics);
        NotificationGateway observingGateway = request -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "HTTP must be outside transaction");
            // A separate connection sees the committed appointment and acquires the released slot lock.
            try (var connection = jdbc.getDataSource().getConnection();
                    var statement = connection.createStatement()) {
                statement.setQueryTimeout(2);
                try (var rows = statement.executeQuery("SELECT COUNT(*) FROM appointments")) {
                    assertTrue(rows.next()); assertEquals(1, rows.getInt(1));
                }
                try (var rows = statement.executeQuery("SELECT slot_id FROM availability_slots WHERE slot_id = 1 FOR UPDATE NOWAIT")) {
                    assertTrue(rows.next());
                }
            } catch (Exception exception) { throw new AssertionError(exception); }
            return actualGateway.send(request);
        };
        var dispatcher = new NotificationDispatcher(observingGateway, outbox, metrics);
        var facade = new BookingService(transactions, appointments, dispatcher, metrics);
        facade.book(1, 1);
        assertEquals("DELIVERED", jdbc.queryForObject("SELECT status FROM notification_outbox", String.class));

        // Simulate a lost HTTP response: sender thinks delivery is still pending.
        jdbc.update("UPDATE notification_outbox SET status = 'PENDING', delivered_at = NULL");
        // A new dispatcher models process restart. Pending state and receiver dedup state are in the DB.
        new NotificationDispatcher(actualGateway, new NotificationOutboxRepository(jdbc), metrics).retryPending();
        assertEquals("DELIVERED", jdbc.queryForObject("SELECT status FROM notification_outbox", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mock_notification_receipts", Integer.class));
    }

    @Test
    void outageRecoversThroughRealHttpWithoutRebooking() {
        transactions.book(1, 1); // Committed pending event; no remote call made in the transaction.
        var failed = new NotificationDispatcher(request -> false, outbox, metrics);
        failed.retryPending();
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM notification_outbox", String.class));
        var recovered = new HttpNotificationGateway(new NotificationProperties(
                "http://localhost:" + port, "local-demo-token", 500, 1000, 3, 0), metrics);
        new NotificationDispatcher(recovered, outbox, metrics).retryPending();
        assertEquals("DELIVERED", jdbc.queryForObject("SELECT status FROM notification_outbox", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM appointments", Integer.class));
    }

    @Test
    void successfulCancellationSendsItsOwnConfirmationAndPendingBookingCanRecover() {
        transactions.book(1, 1);
        int id = jdbc.queryForObject("SELECT appointment_id FROM appointments", Integer.class);
        var actual = new HttpNotificationGateway(new NotificationProperties(
                "http://localhost:" + port, "local-demo-token", 500, 1000, 3, 0), metrics);
        var dispatcher = new NotificationDispatcher(actual, outbox, metrics);
        new BookingService(transactions, appointments, dispatcher, metrics).cancel(1, id);
        assertEquals("CANCELLED", jdbc.queryForObject("SELECT status FROM appointments", String.class));
        assertEquals("CANCELLATION_CONFIRMATION", jdbc.queryForObject(
                "SELECT notification_type FROM mock_notification_receipts", String.class));
        dispatcher.retryPending();
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mock_notification_receipts", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE status = 'PENDING'", Integer.class));
    }

    @Test
    void unexpectedGatewayExceptionCannotUndoACommittedBooking() {
        NotificationGateway broken = request -> { throw new IllegalStateException("test failure"); };
        var dispatcher = new NotificationDispatcher(broken, outbox, metrics);
        assertDoesNotThrow(() -> new BookingService(transactions, appointments, dispatcher, metrics).book(1, 1));
        assertEquals("BOOKED", jdbc.queryForObject("SELECT status FROM appointments", String.class));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM notification_outbox", String.class));
    }

    @Test
    void healthAndMetricsAreReachableAsJson() throws Exception {
        var health = get("/health");
        assertEquals(200, health.statusCode());
        assertTrue(health.body().contains("UP"));
        assertTrue(health.headers().firstValue("content-type").orElse("").contains("application/json"));
        var metric = get("/metrics");
        assertEquals(200, metric.statusCode());
        assertTrue(metric.body().contains("bookings_total"));
    }

    private HttpResponse<String> post(String json, String token) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url("/internal/notifications")))
                .header("Content-Type", "application/json").header("X-Notification-Token", token)
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url(path))).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private String url(String path) { return "http://localhost:" + port + path; }
}
