package com.cmpe172.game_coaching_booking;

import static org.junit.jupiter.api.Assertions.*;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.cmpe172.game_coaching_booking.exception.SlotConflictException;
import com.cmpe172.game_coaching_booking.observability.SchedulerMetrics;
import com.cmpe172.game_coaching_booking.service.BookingService;

/** Real HTTP booking + real database + unreachable notification HTTP endpoint. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationOutageIntegrationTest {
    @Autowired BookingService booking;
    @Autowired JdbcTemplate jdbc;
    @Autowired SchedulerMetrics metrics;
    @Value("${local.server.port}") int port;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM notification_outbox");
        jdbc.update("DELETE FROM mock_notification_receipts");
        jdbc.update("DELETE FROM appointments");
    }

    @Test
    void bookingAndCancellationStillSucceedWhenNotificationServiceIsDown() throws Exception {
        var http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        assertEquals(302, post(http, "/login", "username=player1&password=password").statusCode());
        var response = post(http, "/customer/appointments", "slotId=1");
        assertEquals(302, response.statusCode());
        assertTrue(response.headers().firstValue("location").orElseThrow().contains("confirmation"));
        assertEquals("BOOKED", jdbc.queryForObject("SELECT status FROM appointments", String.class));
        assertEquals(1, pending());

        int id = jdbc.queryForObject("SELECT appointment_id FROM appointments", Integer.class);
        assertEquals(302, post(http, "/customer/appointments/" + id + "/cancel", "").statusCode());
        assertEquals("CANCELLED", jdbc.queryForObject("SELECT status FROM appointments", String.class));
        assertEquals(2, pending());
        var page = http.send(HttpRequest.newBuilder(URI.create(url("/customer/appointments"))).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("CANCELLED"));

        // Regression: cancellation frees the slot while preserving both notifications/history.
        assertEquals(302, post(http, "/customer/appointments", "slotId=1").statusCode());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM appointments", Integer.class));
        assertEquals(3, pending());
    }

    @Test
    void conflictingBookingDoesNotQueueAnotherNotification() {
        booking.book(1, 1);
        long before = ((Number) metrics.snapshot().get("failed_bookings_total")).longValue();
        assertThrows(SlotConflictException.class, () -> booking.book(2, 1));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM appointments", Integer.class));
        assertEquals(1, pending());
        assertEquals(before + 1, ((Number) metrics.snapshot().get("failed_bookings_total")).longValue());
    }

    @Test
    void failedOutboxWriteRollsBackTheAppointmentToo() {
        jdbc.execute("ALTER TABLE notification_outbox ADD CONSTRAINT reject_test_event CHECK (notification_id = 'never-valid')");
        try {
            assertThrows(DataIntegrityViolationException.class, () -> booking.book(1, 1));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM appointments", Integer.class));
            assertEquals(0, pending());
        } finally {
            jdbc.execute("ALTER TABLE notification_outbox DROP CONSTRAINT reject_test_event");
        }
    }

    private int pending() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE status = 'PENDING'", Integer.class);
    }
    private String url(String path) { return "http://localhost:" + port + path; }
    private HttpResponse<String> post(HttpClient http, String path, String form) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url(path)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
