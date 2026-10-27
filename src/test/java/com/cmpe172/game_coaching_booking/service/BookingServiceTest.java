package com.cmpe172.game_coaching_booking.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cmpe172.game_coaching_booking.domain.AvailabilitySlot;
import com.cmpe172.game_coaching_booking.exception.ForbiddenException;
import com.cmpe172.game_coaching_booking.exception.SlotConflictException;
import com.cmpe172.game_coaching_booking.repository.AppointmentRepository;
import com.cmpe172.game_coaching_booking.repository.AppointmentRepository.AppointmentState;
import com.cmpe172.game_coaching_booking.repository.AvailabilitySlotRepository;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private AvailabilitySlotRepository slotRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private com.cmpe172.game_coaching_booking.notification.NotificationOutboxRepository outbox;

    private BookingTransactionService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingTransactionService(slotRepository, appointmentRepository, outbox);
    }

    @Test
    void availableSlotCanBeBooked() {
        AvailabilitySlot slot = new AvailabilitySlot(7, 1, 1,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        when(slotRepository.findByIdForUpdate(7)).thenReturn(Optional.of(slot));
        when(appointmentRepository.hasBookedAppointment(7)).thenReturn(false);
        when(appointmentRepository.create(10, 7)).thenReturn(23);
        var request = new com.cmpe172.game_coaching_booking.notification.NotificationRequest(
                "appointment-23-booking-confirmation", "one@example.com",
                com.cmpe172.game_coaching_booking.notification.NotificationType.BOOKING_CONFIRMATION, "Confirmed");
        when(appointmentRepository.notificationFor(23,
                com.cmpe172.game_coaching_booking.notification.NotificationType.BOOKING_CONFIRMATION)).thenReturn(request);

        bookingService.book(10, 7);

        verify(appointmentRepository).create(10, 7);
        verify(outbox).enqueue(request);
    }

    @Test
    void alreadyBookedSlotReturnsConflict() {
        AvailabilitySlot slot = new AvailabilitySlot(7, 1, 1,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        when(slotRepository.findByIdForUpdate(7)).thenReturn(Optional.of(slot));
        when(appointmentRepository.hasBookedAppointment(7)).thenReturn(true);

        assertThrows(SlotConflictException.class, () -> bookingService.book(10, 7));
        verify(appointmentRepository, never()).create(10, 7);
    }

    @Test
    void customerCannotCancelAnotherCustomersAppointment() {
        when(appointmentRepository.findState(4))
                .thenReturn(Optional.of(new AppointmentState(99, "BOOKED")));

        assertThrows(ForbiddenException.class, () -> bookingService.cancel(10, 4));
        verify(appointmentRepository, never()).cancel(4, 10);
    }
}
