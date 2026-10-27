package com.cmpe172.game_coaching_booking.service;

import java.time.LocalDateTime;
import com.cmpe172.game_coaching_booking.notification.NotificationRequest;
import com.cmpe172.game_coaching_booking.notification.NotificationType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.cmpe172.game_coaching_booking.domain.AvailabilitySlot;
import com.cmpe172.game_coaching_booking.exception.ForbiddenException;
import com.cmpe172.game_coaching_booking.exception.InvalidRequestException;
import com.cmpe172.game_coaching_booking.exception.NotFoundException;
import com.cmpe172.game_coaching_booking.exception.SlotConflictException;
import com.cmpe172.game_coaching_booking.repository.AppointmentRepository;
import com.cmpe172.game_coaching_booking.repository.AppointmentRepository.AppointmentState;
import com.cmpe172.game_coaching_booking.repository.AvailabilitySlotRepository;

@Service
public class BookingTransactionService {

    private final AvailabilitySlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final com.cmpe172.game_coaching_booking.notification.NotificationOutboxRepository outbox;

    public BookingTransactionService(AvailabilitySlotRepository slotRepository,
            AppointmentRepository appointmentRepository,
            com.cmpe172.game_coaching_booking.notification.NotificationOutboxRepository outbox) {
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.outbox = outbox;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public NotificationRequest book(int userId, int slotId) {
        if (userId <= 0 || slotId <= 0) {
            throw new InvalidRequestException("A valid user and slot are required");
        }

        // PostgreSQL holds this row lock until the transaction commits or rolls back.
        AvailabilitySlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new NotFoundException("The selected slot was not found"));

        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException("Past slots cannot be booked");
        }
        if (appointmentRepository.hasBookedAppointment(slotId)) {
            throw new SlotConflictException("This slot was already booked by another customer");
        }

        int appointmentId;
        try {
            appointmentId = appointmentRepository.create(userId, slotId);
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            throw new SlotConflictException("This slot was already booked by another customer");
        }
        return enqueue(appointmentId, NotificationType.BOOKING_CONFIRMATION);
    }

    @Transactional
    public NotificationRequest cancel(int userId, int appointmentId) {
        AppointmentState state = appointmentRepository.findState(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
        if (state.userId() != userId) {
            throw new ForbiddenException("You can only cancel your own appointments");
        }
        if (!"BOOKED".equals(state.status())) {
            throw new SlotConflictException("Only a booked appointment can be cancelled");
        }
        if (appointmentRepository.cancel(appointmentId, userId) != 1) {
            throw new SlotConflictException("Only a booked appointment can be cancelled");
        }
        return enqueue(appointmentId, NotificationType.CANCELLATION_CONFIRMATION);
    }

    private NotificationRequest enqueue(int appointmentId, NotificationType type) {
        NotificationRequest request = appointmentRepository.notificationFor(appointmentId, type);
        outbox.enqueue(request);
        return request;
    }
}
