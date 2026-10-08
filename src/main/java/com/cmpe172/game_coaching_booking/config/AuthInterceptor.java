package com.cmpe172.game_coaching_booking.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SessionKeys.USER_ID) == null) {
            response.sendRedirect("/login");
            return false;
        }

        String actualRole = (String) session.getAttribute(SessionKeys.ROLE);
        String requiredRole = request.getRequestURI().startsWith("/provider/")
                ? "PROVIDER"
                : "CUSTOMER";

        if (!requiredRole.equals(actualRole)) {
            response.sendError(HttpStatus.FORBIDDEN.value(),
                    "This page requires the " + requiredRole + " role");
            return false;
        }

        return true;
    }
}
