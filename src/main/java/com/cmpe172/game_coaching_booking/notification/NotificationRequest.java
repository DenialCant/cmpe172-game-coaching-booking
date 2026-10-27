package com.cmpe172.game_coaching_booking.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** One self-contained command; no entities or follow-up lookups cross HTTP. */
public record NotificationRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[a-zA-Z0-9-]+") String notificationId,
        @NotBlank @Email @Size(max = 320) String recipient,
        @NotNull NotificationType type,
        @NotBlank @Size(max = 1000) String message) {
}
