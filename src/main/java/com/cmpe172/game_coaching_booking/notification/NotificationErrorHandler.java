package com.cmpe172.game_coaching_booking.notification;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Avoid logging rejected recipient/message values through MVC's default resolver. */
@RestControllerAdvice(assignableTypes = MockNotificationController.class)
public class NotificationErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationErrorHandler.class);

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalidNotification(Exception ignored) {
        log.warn("Notification request rejected reason=invalid-payload");
        return ResponseEntity.badRequest().body(Map.of("status", "INVALID_NOTIFICATION"));
    }
}
