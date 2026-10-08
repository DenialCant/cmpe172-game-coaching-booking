package com.cmpe172.game_coaching_booking.domain;

import java.time.LocalDateTime;

public class AvailabilitySlot {

    private int slotId;
    private int providerId;
    private int serviceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public AvailabilitySlot(
            int slotId,
            int providerId,
            int serviceId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        this.slotId = slotId;
        this.providerId = providerId;
        this.serviceId = serviceId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getSlotId() {
        return slotId;
    }

    public int getProviderId() {
        return providerId;
    }

    public int getServiceId() {
        return serviceId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}