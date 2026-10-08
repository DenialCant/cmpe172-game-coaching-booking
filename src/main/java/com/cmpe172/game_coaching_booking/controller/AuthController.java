package com.cmpe172.game_coaching_booking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.cmpe172.game_coaching_booking.config.SessionKeys;
import com.cmpe172.game_coaching_booking.domain.UserAccount;
import com.cmpe172.game_coaching_booking.dto.LoginForm;
import com.cmpe172.game_coaching_booking.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginForm loginForm,
            BindingResult bindingResult, HttpServletRequest request, Model model) {

        if (bindingResult.hasErrors()) {
            return "login";
        }

        UserAccount user = authService.authenticate(loginForm.getUsername(), loginForm.getPassword())
                .orElse(null);
        if (user == null) {
            model.addAttribute("loginError", "Incorrect username or password");
            return "login";
        }

        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(SessionKeys.USER_ID, user.getUserId());
        session.setAttribute(SessionKeys.USERNAME, user.getUsername());
        session.setAttribute(SessionKeys.FULL_NAME, user.getFullName());
        session.setAttribute(SessionKeys.ROLE, user.getRole());
        if (user.getProviderId() != null) {
            session.setAttribute(SessionKeys.PROVIDER_ID, user.getProviderId());
        }

        return "PROVIDER".equals(user.getRole())
                ? "redirect:/provider/dashboard"
                : "redirect:/slots";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout";
    }
}
