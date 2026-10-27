package com.cmpe172.game_coaching_booking.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.cmpe172.game_coaching_booking.dto.AppointmentView;
import com.cmpe172.game_coaching_booking.notification.NotificationDispatcher;
import com.cmpe172.game_coaching_booking.notification.NotificationRequest;
import com.cmpe172.game_coaching_booking.observability.SchedulerMetrics;
import com.cmpe172.game_coaching_booking.repository.AppointmentRepository;

@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private final BookingTransactionService transactions;
    private final AppointmentRepository appointments;
    private final NotificationDispatcher dispatcher;
    private final SchedulerMetrics metrics;

    public BookingService(BookingTransactionService transactions, AppointmentRepository appointments,
            NotificationDispatcher dispatcher, SchedulerMetrics metrics) {
        this.transactions = transactions;
        this.appointments = appointments;
        this.dispatcher = dispatcher;
        this.metrics = metrics;
    }

    // Suspend any caller transaction. The separate transaction bean MUST commit
    // before it returns; notification HTTP calls therefore hold no transaction/slot lock.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void book(int userId, int slotId) {
        long started = System.nanoTime();
        try {
            NotificationRequest request;
            try {
                request = transactions.book(userId, slotId);
            } catch (RuntimeException exception) {
                metrics.bookingFailed();
                log.warn("Booking rejected slotId={} reason={}", slotId, exception.getClass().getSimpleName());
                throw exception;
            }
            metrics.bookingSucceeded();
            log.info("Booking committed slotId={} notificationId={}", slotId, request.notificationId());
            dispatcher.dispatch(request);
        } finally {
            // Includes notification wait time and failed attempts; service-level latency.
            metrics.recordBookingLatency(System.nanoTime() - started);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void cancel(int userId, int appointmentId) {
        NotificationRequest request = transactions.cancel(userId, appointmentId);
        metrics.cancellationSucceeded();
        log.info("Cancellation committed appointmentId={} notificationId={}", appointmentId, request.notificationId());
        dispatcher.dispatch(request);
    }

    @Transactional
    public List<AppointmentView> customerAppointments(int userId) {
        appointments.markPastAppointmentsCompleted();
        return appointments.findForCustomer(userId);
    }

    @Transactional
    public List<AppointmentView> providerAppointments(int providerId) {
        appointments.markPastAppointmentsCompleted();
        return appointments.findForProvider(providerId);
    }
}
