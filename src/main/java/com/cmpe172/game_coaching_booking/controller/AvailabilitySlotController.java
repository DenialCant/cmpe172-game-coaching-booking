package com.cmpe172.game_coaching_booking.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cmpe172.game_coaching_booking.dto.AvailableSlotView;
import com.cmpe172.game_coaching_booking.dto.PageResult;
import com.cmpe172.game_coaching_booking.repository.AvailabilitySlotRepository;
import com.cmpe172.game_coaching_booking.service.AvailabilitySlotService;

@Controller
public class AvailabilitySlotController {

    private final AvailabilitySlotService service;
    private final AvailabilitySlotRepository repository;

    public AvailabilitySlotController(AvailabilitySlotService service,
            AvailabilitySlotRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping("/slots")
    public String getSlots(
            @RequestParam(required = false) Integer providerId,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model) {

        PageResult<AvailableSlotView> result = service.browse(providerId, serviceId, date, page, size);
        model.addAttribute("result", result);
        model.addAttribute("providers", repository.findAllProviders());
        model.addAttribute("services", repository.findAllServices());
        model.addAttribute("providerId", providerId);
        model.addAttribute("serviceId", serviceId);
        model.addAttribute("date", date);
        return "slots";
    }

    @GetMapping("/api/slots")
    @ResponseBody
    public PageResult<AvailableSlotView> getSlotsApi(
            @RequestParam(required = false) Integer providerId,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return service.browse(providerId, serviceId, date, page, size);
    }
}
