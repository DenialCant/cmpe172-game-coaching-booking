package com.cmpe172.game_coaching_booking.notification;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationOutboxRepository {
    private final JdbcTemplate jdbc;

    public NotificationOutboxRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Called INSIDE the booking/cancellation transaction: both writes commit together. */
    public void enqueue(NotificationRequest request) {
        jdbc.update("""
                INSERT INTO notification_outbox (notification_id, recipient, notification_type, message)
                VALUES (?, ?, ?, ?)
                """, request.notificationId(), request.recipient(), request.type().name(), request.message());
    }

    public List<NotificationRequest> pending() {
        return jdbc.query("""
                SELECT notification_id, recipient, notification_type, message
                FROM notification_outbox WHERE status = 'PENDING'
                ORDER BY created_at, notification_id LIMIT 25
                """, (rs, n) -> new NotificationRequest(rs.getString("notification_id"),
                        rs.getString("recipient"), NotificationType.valueOf(rs.getString("notification_type")),
                        rs.getString("message")));
    }

    public void markDelivered(String id) {
        jdbc.update("""
                UPDATE notification_outbox SET status = 'DELIVERED', delivered_at = CURRENT_TIMESTAMP
                WHERE notification_id = ? AND status = 'PENDING'
                """, id);
    }
}
