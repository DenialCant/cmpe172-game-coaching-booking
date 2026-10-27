package com.cmpe172.game_coaching_booking;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.cmpe172.game_coaching_booking.exception.SlotConflictException;
import com.cmpe172.game_coaching_booking.service.BookingService;

@EnabledIfEnvironmentVariable(named = "POSTGRES_TEST_URL", matches = ".+")
@SpringBootTest
@ActiveProfiles("postgres-test")
class PostgresIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired BookingService booking;
    int slot;
    int firstUser;
    int secondUser;

    @BeforeEach
    void prepare() {
        // Only use a dedicated disposable database: these statements delete test data.
        jdbc.update("DELETE FROM notification_outbox");
        jdbc.update("DELETE FROM mock_notification_receipts");
        jdbc.update("DELETE FROM appointments");
        slot = jdbc.queryForObject("SELECT MIN(slot_id) FROM availability_slots", Integer.class);
        jdbc.update("UPDATE availability_slots SET start_time = CURRENT_TIMESTAMP + INTERVAL '1 day', end_time = CURRENT_TIMESTAMP + INTERVAL '1 day 1 hour', active = TRUE WHERE slot_id = ?", slot);
        firstUser = jdbc.queryForObject("SELECT user_id FROM users WHERE username = 'player1'", Integer.class);
        secondUser = jdbc.queryForObject("SELECT user_id FROM users WHERE username = 'player2'", Integer.class);
    }

    @Test
    void realPostgresBookingCommitsDuringNotificationOutageAndCanBeRebookedAfterCancel() {
        booking.book(firstUser, slot);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM appointments WHERE status = 'BOOKED'", Integer.class));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM notification_outbox", String.class));
        int id = jdbc.queryForObject("SELECT appointment_id FROM appointments", Integer.class);
        booking.cancel(firstUser, id);
        booking.book(secondUser, slot);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM appointments WHERE status = 'BOOKED'", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox", Integer.class));
    }

    @Test
    void realPostgresRowLockAllowsOnlyOneConcurrentBooking() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> a = pool.submit(() -> attempt(firstUser, ready, start));
            Future<Boolean> b = pool.submit(() -> attempt(secondUser, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successes = (a.get(10, TimeUnit.SECONDS) ? 1 : 0) + (b.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM appointments WHERE status = 'BOOKED'", Integer.class));
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    private boolean attempt(int user, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("Start latch timed out"); }
        try { booking.book(user, slot); return true; }
        catch (SlotConflictException conflict) { return false; }
    }
}
