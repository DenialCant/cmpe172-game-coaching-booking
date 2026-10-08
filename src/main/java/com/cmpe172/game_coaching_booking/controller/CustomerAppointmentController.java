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
import com.cmpe172.game_coaching_booking.dto.BookAppointmentForm;
import com.cmpe172.game_coaching_booking.exception.InvalidRequestException;
import com.cmpe172.game_coaching_booking.service.BookingService;

import jakarta.validation.Valid;

@Controller
public class CustomerAppointmentController {

    private final BookingService bookingService;

    public CustomerAppointmentController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/customer/appointments")
    public String book(@Valid @ModelAttribute BookAppointmentForm form,
            BindingResult bindingResult,
            @SessionAttribute(SessionKeys.USER_ID) int userId) {
        if (bindingResult.hasErrors()) {
            throw new InvalidRequestException("A valid slot is required");
        }
        bookingService.book(userId, form.getSlotId());
        return "redirect:/customer/confirmation";
    }

    @GetMapping("/customer/confirmation")
    public String confirmation() {
        return "confirmation";
    }

    @GetMapping("/customer/appointments")
    public String appointments(@SessionAttribute(SessionKeys.USER_ID) int userId, Model model) {
        model.addAttribute("appointments", bookingService.customerAppointments(userId));
        return "customer-appointments";
    }

    @PostMapping("/customer/appointments/{appointmentId}/cancel")
    public String cancel(@PathVariable int appointmentId,
            @SessionAttribute(SessionKeys.USER_ID) int userId) {
        bookingService.cancel(userId, appointmentId);
        return "redirect:/customer/appointments?cancelled";
    }
}
