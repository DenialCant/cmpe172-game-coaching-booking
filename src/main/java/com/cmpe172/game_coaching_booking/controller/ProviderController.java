package com.cmpe172.game_coaching_booking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.cmpe172.game_coaching_booking.config.SessionKeys;
import com.cmpe172.game_coaching_booking.dto.AvailabilityForm;
import com.cmpe172.game_coaching_booking.repository.AvailabilitySlotRepository;
import com.cmpe172.game_coaching_booking.service.BookingService;
import com.cmpe172.game_coaching_booking.service.ProviderService;

import jakarta.validation.Valid;

@Controller
public class ProviderController {

    private final ProviderService providerService;
    private final BookingService bookingService;
    private final AvailabilitySlotRepository slotRepository;

    public ProviderController(ProviderService providerService, BookingService bookingService,
            AvailabilitySlotRepository slotRepository) {
        this.providerService = providerService;
        this.bookingService = bookingService;
        this.slotRepository = slotRepository;
    }

    @GetMapping("/provider/dashboard")
    public String dashboard(@SessionAttribute(SessionKeys.PROVIDER_ID) int providerId, Model model) {
        if (!model.containsAttribute("availabilityForm")) {
            model.addAttribute("availabilityForm", new AvailabilityForm());
        }
        populateDashboard(providerId, model);
        return "provider-dashboard";
    }

    @PostMapping("/provider/slots")
    public String createSlot(@Valid @ModelAttribute AvailabilityForm availabilityForm,
            BindingResult bindingResult,
            @SessionAttribute(SessionKeys.PROVIDER_ID) int providerId,
            Model model) {
        if (bindingResult.hasErrors()) {
            populateDashboard(providerId, model);
            return "provider-dashboard";
        }
        providerService.createSlot(providerId, availabilityForm);
        return "redirect:/provider/dashboard?created";
    }

    @PostMapping("/provider/slots/{slotId}/remove")
    public String removeSlot(@PathVariable int slotId,
            @SessionAttribute(SessionKeys.PROVIDER_ID) int providerId) {
        providerService.removeSlot(providerId, slotId);
        return "redirect:/provider/dashboard?removed";
    }

    private void populateDashboard(int providerId, Model model) {
        model.addAttribute("services", slotRepository.findServicesForProvider(providerId));
        model.addAttribute("slots", slotRepository.findForProvider(providerId));
        model.addAttribute("appointments", bookingService.providerAppointments(providerId));
    }
}
