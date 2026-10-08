package com.cmpe172.game_coaching_booking.dto;

public class HomeDto {

    private String application;
    private int providerCount;
    private int serviceCount;
    private int availableSlotCount;

    public HomeDto(
            String application,
            int providerCount,
            int serviceCount,
            int availableSlotCount) {

        this.application = application;
        this.providerCount = providerCount;
        this.serviceCount = serviceCount;
        this.availableSlotCount = availableSlotCount;
    }

    public String getApplication() {
        return application;
    }

    public int getProviderCount() {
        return providerCount;
    }

    public int getServiceCount() {
        return serviceCount;
    }

    public int getAvailableSlotCount() {
        return availableSlotCount;
    }
}