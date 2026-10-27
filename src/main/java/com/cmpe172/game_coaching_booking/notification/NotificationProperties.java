package com.cmpe172.game_coaching_booking.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "notifications")
public record NotificationProperties(
        @NotBlank String baseUrl,
        @NotBlank String token,
        @Min(1) @Max(10000) int connectTimeoutMs,
        @Min(1) @Max(10000) int readTimeoutMs,
        @Min(1) @Max(5) int maxAttempts,
        @Min(0) @Max(1000) int retryDelayMs) {
}
