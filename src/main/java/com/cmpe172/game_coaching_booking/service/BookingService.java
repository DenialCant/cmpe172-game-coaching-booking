package com.cmpe172.game_coaching_booking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.cmpe172.game_coaching_booking.domain.AvailabilitySlot;
import com.cmpe172.game_coaching_booking.dto.AppointmentView;
import com.cmpe172.game_coaching_booking.exception.ForbiddenException;
import com.cmpe172.game_coaching_booking.exception.InvalidRequestException;
import com.cmpe172.game_coaching_booking.exception.NotFoundException;
import com.cmpe172.game_coaching_booking.exception.SlotConflictException;
import com.cmpe172.game_coaching_booking.repository.AppointmentRepository;
import com.cmpe172.game_coaching_booking.repository.AppointmentRepository.AppointmentState;
import com.cmpe172.game_coaching_booking.repository.AvailabilitySlotRepository;

@Service
public class BookingService {

    private final AvailabilitySlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;

    public BookingService(AvailabilitySlotRepository slotRepository,
            AppointmentRepository appointmentRepository) {
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void book(int userId, int slotId) {
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

        try {
            appointmentRepository.create(userId, slotId);
        } catch (DataIntegrityViolationException exception) {
            throw new SlotConflictException("This slot was already booked by another customer");
        }
    }

    @Transactional
    public void cancel(int userId, int appointmentId) {
        AppointmentState state = appointmentRepository.findState(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
        if (state.userId() != userId) {
            throw new ForbiddenException("You can only cancel your own appointments");
        }
        if (!"BOOKED".equals(state.status())) {
            throw new SlotConflictException("Only a booked appointment can be cancelled");
        }
        appointmentRepository.cancel(appointmentId, userId);
    }

    @Transactional
    public List<AppointmentView> customerAppointments(int userId) {
        appointmentRepository.markPastAppointmentsCompleted();
        return appointmentRepository.findForCustomer(userId);
    }

    @Transactional
    public List<AppointmentView> providerAppointments(int providerId) {
        appointmentRepository.markPastAppointmentsCompleted();
        return appointmentRepository.findForProvider(providerId);
    }
}
