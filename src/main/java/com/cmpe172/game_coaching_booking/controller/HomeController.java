package com.cmpe172.game_coaching_booking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cmpe172.game_coaching_booking.dto.HomeDto;
import com.cmpe172.game_coaching_booking.service.HomeService;

@Controller
public class HomeController {

    private final HomeService service;

    public HomeController(HomeService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String getHome(Model model) {
        model.addAttribute("summary", service.getHomeSummary());
        return "home";
    }

    @GetMapping("/api/home")
    @ResponseBody
    public HomeDto getHomeApi() {
        return service.getHomeSummary();
    }
}
