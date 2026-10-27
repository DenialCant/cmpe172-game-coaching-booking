package com.cmpe172.game_coaching_booking.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.cmpe172.game_coaching_booking.observability.SchedulerMetrics;

@Service
public class NotificationDispatcher {
    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private final NotificationGateway gateway;
    private final NotificationOutboxRepository outbox;
    private final SchedulerMetrics metrics;

    public NotificationDispatcher(NotificationGateway gateway, NotificationOutboxRepository outbox,
            SchedulerMetrics metrics) {
        this.gateway = gateway;
        this.outbox = outbox;
        this.metrics = metrics;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void dispatch(NotificationRequest request) {
        try {
            if (gateway.send(request)) {
                outbox.markDelivered(request.notificationId());
                metrics.notificationDelivered();
                log.info("Notification delivered notificationId={} type={}", request.notificationId(), request.type());
                return;
            }
        } catch (RuntimeException exception) {
            // Even a failure to record remote success must never invalidate an already committed booking.
            // The pending event can be replayed safely because the receiver deduplicates its stable ID.
            log.error("Notification dispatch failed notificationId={} reason={}",
                    request.notificationId(), exception.getClass().getSimpleName());
        }
        metrics.notificationDeferred();
        log.warn("Notification deferred notificationId={} recovery=outbox-retry", request.notificationId());
    }

    @Scheduled(fixedDelayString = "${notifications.retry-interval-ms:30000}",
            initialDelayString = "${notifications.retry-initial-delay-ms:30000}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void retryPending() {
        try {
            // Runs outside a transaction. HTTP calls do not keep a DB transaction open.
            for (NotificationRequest request : outbox.pending()) {
                dispatch(request);
            }
        } catch (RuntimeException exception) {
            log.error("Outbox recovery scan failed reason={}", exception.getClass().getSimpleName());
        }
    }
}
