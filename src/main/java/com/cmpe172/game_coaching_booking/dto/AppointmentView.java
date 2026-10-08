package com.cmpe172.game_coaching_booking.dto;

import java.time.LocalDateTime;

public record AppointmentView(
        int appointmentId,
        String customerName,
        String providerName,
        String serviceName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status) {
}
