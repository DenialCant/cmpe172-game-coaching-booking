package com.cmpe172.game_coaching_booking.notification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

/** A second controller stands in for the external messaging service; no email is sent. */
@RestController
@ConditionalOnProperty(name = "notifications.stub.enabled", havingValue = "true", matchIfMissing = true)
public class MockNotificationController {
    private static final Logger log = LoggerFactory.getLogger(MockNotificationController.class);
    private final JdbcTemplate jdbc;
    private final NotificationProperties properties;

    public MockNotificationController(JdbcTemplate jdbc, NotificationProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    @PostMapping("/internal/notifications")
    public ResponseEntity<Map<String, String>> accept(
            @RequestHeader(value = "X-Notification-Token", defaultValue = "") String token,
            @Valid @RequestBody NotificationRequest request) {
        if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                properties.token().getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "FORBIDDEN"));
        }
        try {
            // Primary key makes concurrent retries and replays after a restart idempotent.
            jdbc.update("""
                    INSERT INTO mock_notification_receipts
                        (notification_id, recipient, notification_type, message)
                    VALUES (?, ?, ?, ?)
                    """, request.notificationId(), request.recipient(), request.type().name(), request.message());
            log.info("Mock notification accepted notificationId={} type={}", request.notificationId(), request.type());
            return ResponseEntity.ok(Map.of("status", "ACCEPTED", "notificationId", request.notificationId()));
        } catch (DuplicateKeyException duplicate) {
            Integer samePayload = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM mock_notification_receipts
                    WHERE notification_id = ? AND recipient = ? AND notification_type = ? AND message = ?
                    """, Integer.class, request.notificationId(), request.recipient(), request.type().name(), request.message());
            if (samePayload != null && samePayload == 1) {
                return ResponseEntity.ok(Map.of("status", "DUPLICATE", "notificationId", request.notificationId()));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "ID_REUSED_WITH_DIFFERENT_PAYLOAD"));
        }
    }
}
