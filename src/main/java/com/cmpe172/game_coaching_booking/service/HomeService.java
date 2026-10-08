package com.cmpe172.game_coaching_booking.service;

import org.springframework.stereotype.Service;

import com.cmpe172.game_coaching_booking.dto.HomeDto;
import com.cmpe172.game_coaching_booking.repository.HomeRepository;

@Service
public class HomeService {

    private final HomeRepository repository;

    public HomeService(HomeRepository repository) {
        this.repository = repository;
    }

    public HomeDto getHomeSummary() {
        int providerCount = repository.countProviders();
        int serviceCount = repository.countServices();
        int availableSlotCount = repository.countAvailableSlots();

        return new HomeDto(
                "Game Coaching Booking System",
                providerCount,
                serviceCount,
                availableSlotCount
        );
    }
}