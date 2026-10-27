package com.cmpe172.game_coaching_booking.observability;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;

/** Process-lifetime counters, intentionally independent of database availability. */
@Component
public class SchedulerMetrics {
    private final LongAdder bookings = new LongAdder();
    private final LongAdder failedBookings = new LongAdder();
    private final LongAdder cancellations = new LongAdder();
    private final LongAdder bookingNanos = new LongAdder();
    private final LongAdder bookingCalls = new LongAdder();
    private final LongAdder notificationAttempts = new LongAdder();
    private final LongAdder notificationsDelivered = new LongAdder();
    private final LongAdder notificationsDeferred = new LongAdder();

    public void bookingSucceeded() { bookings.increment(); }
    public void bookingFailed() { failedBookings.increment(); }
    public void cancellationSucceeded() { cancellations.increment(); }
    public void recordBookingLatency(long nanos) {
        bookingNanos.add(nanos);
        bookingCalls.increment();
    }
    public void notificationAttempted() { notificationAttempts.increment(); }
    public void notificationDelivered() { notificationsDelivered.increment(); }
    public void notificationDeferred() { notificationsDeferred.increment(); }

    public Map<String, Object> snapshot() {
        long calls = bookingCalls.sum();
        return Map.of(
                "scope", "since_process_start",
                "bookings_total", bookings.sum(),
                "failed_bookings_total", failedBookings.sum(),
                "cancellations_total", cancellations.sum(),
                "average_booking_latency_ms", calls == 0 ? 0.0 : bookingNanos.sum() / 1_000_000.0 / calls,
                "notification_http_attempts_total", notificationAttempts.sum(),
                "notification_delivery_completions_total", notificationsDelivered.sum(),
                "notification_deferrals_total", notificationsDeferred.sum());
    }
}
