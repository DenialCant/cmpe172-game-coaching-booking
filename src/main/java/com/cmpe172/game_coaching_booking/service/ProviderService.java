package com.cmpe172.game_coaching_booking.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cmpe172.game_coaching_booking.dto.AvailabilityForm;
import com.cmpe172.game_coaching_booking.exception.ForbiddenException;
import com.cmpe172.game_coaching_booking.exception.InvalidRequestException;
import com.cmpe172.game_coaching_booking.exception.NotFoundException;
import com.cmpe172.game_coaching_booking.exception.SlotConflictException;
import com.cmpe172.game_coaching_booking.repository.AvailabilitySlotRepository;

@Service
public class ProviderService {

    private final AvailabilitySlotRepository slotRepository;

    public ProviderService(AvailabilitySlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Transactional
    public void createSlot(int providerId, AvailabilityForm form) {
        if (!slotRepository.serviceBelongsToProvider(form.getServiceId(), providerId)) {
            throw new ForbiddenException("The selected service does not belong to this provider");
        }
        if (form.getStartTime() == null || form.getEndTime() == null) {
            throw new InvalidRequestException("Start and end time are required");
        }
        if (!form.getStartTime().isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException("Availability must start in the future");
        }
        if (!form.getEndTime().isAfter(form.getStartTime())) {
            throw new InvalidRequestException("End time must be after start time");
        }
        if (slotRepository.hasOverlap(providerId, form.getStartTime(), form.getEndTime())) {
            throw new SlotConflictException("This availability overlaps another active slot");
        }
        slotRepository.create(providerId, form.getServiceId(), form.getStartTime(), form.getEndTime());
    }

    @Transactional
    public void removeSlot(int providerId, int slotId) {
        if (!slotRepository.existsOwnedSlot(slotId, providerId)) {
            throw new NotFoundException("Availability slot not found");
        }
        if (slotRepository.deactivate(slotId, providerId) == 0) {
            throw new SlotConflictException("A booked availability slot cannot be removed");
        }
    }
}
