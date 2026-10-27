package com.cmpe172.game_coaching_booking.notification;

/** A replaceable distribution boundary. False means defer delivery. */
public interface NotificationGateway {
    boolean send(NotificationRequest request);
}
