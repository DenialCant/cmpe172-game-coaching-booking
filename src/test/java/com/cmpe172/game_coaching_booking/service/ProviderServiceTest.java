package com.cmpe172.game_coaching_booking.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cmpe172.game_coaching_booking.dto.AvailabilityForm;
import com.cmpe172.game_coaching_booking.exception.InvalidRequestException;
import com.cmpe172.game_coaching_booking.repository.AvailabilitySlotRepository;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private AvailabilitySlotRepository slotRepository;

    private ProviderService providerService;

    @BeforeEach
    void setUp() {
        providerService = new ProviderService(slotRepository);
    }

    @Test
    void endTimeMustBeAfterStartTime() {
        AvailabilityForm form = new AvailabilityForm();
        form.setServiceId(1);
        form.setStartTime(LocalDateTime.now().plusDays(1));
        form.setEndTime(LocalDateTime.now().plusHours(1));
        when(slotRepository.serviceBelongsToProvider(1, 3)).thenReturn(true);

        assertThrows(InvalidRequestException.class, () -> providerService.createSlot(3, form));
    }
}
