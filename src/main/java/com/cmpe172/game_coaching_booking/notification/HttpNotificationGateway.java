package com.cmpe172.game_coaching_booking.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import com.cmpe172.game_coaching_booking.observability.SchedulerMetrics;

@Component
public class HttpNotificationGateway implements NotificationGateway {
    private static final Logger log = LoggerFactory.getLogger(HttpNotificationGateway.class);
    private final RestClient client;
    private final NotificationProperties properties;
    private final SchedulerMetrics metrics;

    public HttpNotificationGateway(NotificationProperties properties, SchedulerMetrics metrics) {
        this.properties = properties;
        this.metrics = metrics;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeoutMs());
        factory.setReadTimeout(properties.readTimeoutMs());
        client = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }

    @Override
    public boolean send(NotificationRequest request) {
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            metrics.notificationAttempted();
            try {
                // The same notificationId and payload are reused on EVERY attempt.
                client.post().uri("/internal/notifications")
                        .header("X-Notification-Token", properties.token())
                        .body(request).retrieve().toBodilessEntity();
                return true;
            } catch (RestClientException exception) {
                log.warn("Notification attempt failed notificationId={} attempt={} reason={}",
                        request.notificationId(), attempt, exception.getClass().getSimpleName());
                // Validation/auth errors need configuration correction, not immediate retries.
                if (exception instanceof RestClientResponseException response
                        && response.getStatusCode().is4xxClientError()
                        && response.getStatusCode().value() != 429) {
                    break;
                }
                if (attempt < properties.maxAttempts()) {
                    try {
                        Thread.sleep((long) properties.retryDelayMs() * attempt);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return false;
    }
}
