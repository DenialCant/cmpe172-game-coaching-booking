package com.cmpe172.game_coaching_booking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.cmpe172.game_coaching_booking.exception.SlotConflictException;
import com.cmpe172.game_coaching_booking.service.BookingService;

@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void exactlyOneOfTwoConcurrentBookingsSucceeds() throws Exception {
        jdbcTemplate.update("DELETE FROM appointments");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> firstAttempt = attempt(1, ready, start);
        Callable<Boolean> secondAttempt = attempt(2, ready, start);
        Future<Boolean> first = executor.submit(firstAttempt);
        Future<Boolean> second = executor.submit(secondAttempt);

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        List<Boolean> results = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
        executor.shutdownNow();

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointments WHERE slot_id = 1 AND status = 'BOOKED'",
                Integer.class));
    }

    private Callable<Boolean> attempt(int userId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
                bookingService.book(userId, 1);
                return true;
            } catch (SlotConflictException exception) {
                return false;
            }
        };
    }
}
