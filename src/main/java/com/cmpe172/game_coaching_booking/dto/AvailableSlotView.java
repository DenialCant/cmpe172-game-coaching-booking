package com.cmpe172.game_coaching_booking.dto;

import java.time.LocalDateTime;

public record AvailableSlotView(
        int slotId,
        int providerId,
        String providerName,
        int serviceId,
        String serviceName,
        int durationMinutes,
        LocalDateTime startTime,
        LocalDateTime endTime) {
}
