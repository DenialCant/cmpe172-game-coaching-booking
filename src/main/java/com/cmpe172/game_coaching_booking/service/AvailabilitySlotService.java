package com.cmpe172.game_coaching_booking.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.cmpe172.game_coaching_booking.dto.AvailableSlotView;
import com.cmpe172.game_coaching_booking.dto.PageResult;
import com.cmpe172.game_coaching_booking.repository.AvailabilitySlotRepository;

@Service
public class AvailabilitySlotService {

    private final AvailabilitySlotRepository repository;

    public AvailabilitySlotService(AvailabilitySlotRepository repository) {
        this.repository = repository;
    }

    public PageResult<AvailableSlotView> browse(Integer providerId, Integer serviceId,
            LocalDate date, int requestedPage, int requestedSize) {
        int page = Math.max(requestedPage, 0);
        int size = Math.min(Math.max(requestedSize, 1), 20);
        long total = repository.countAvailable(providerId, serviceId, date);
        return new PageResult<>(
                repository.findAvailable(providerId, serviceId, date, size, page * size),
                page, size, total);
    }
}
