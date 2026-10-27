package com.cmpe172.game_coaching_booking.repository;

import java.util.List;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import com.cmpe172.game_coaching_booking.notification.NotificationRequest;
import com.cmpe172.game_coaching_booking.notification.NotificationType;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.cmpe172.game_coaching_booking.dto.AppointmentView;

@Repository
public class AppointmentRepository {

    public record AppointmentState(int userId, String status) {
    }

    private final JdbcTemplate jdbcTemplate;

    public AppointmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasBookedAppointment(int slotId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointments WHERE slot_id = ? AND status = 'BOOKED'",
                Integer.class, slotId);
        return count != null && count > 0;
    }

    public int create(int userId, int slotId) {
        var keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO appointments (user_id, slot_id, status) VALUES (?, ?, 'BOOKED')",
                    new String[] { "appointment_id" });
            statement.setInt(1, userId);
            statement.setInt(2, slotId);
            return statement;
        }, keys);
        Number id = keys.getKey();
        if (id == null) { throw new IllegalStateException("Appointment ID was not returned"); }
        return id.intValue();
    }

    public NotificationRequest notificationFor(int appointmentId, NotificationType type) {
        return jdbcTemplate.queryForObject("""
                SELECT customer.email, service.service_name, slot.start_time
                FROM appointments appointment
                JOIN users customer ON customer.user_id = appointment.user_id
                JOIN availability_slots slot ON slot.slot_id = appointment.slot_id
                JOIN services service ON service.service_id = slot.service_id
                WHERE appointment.appointment_id = ?
                """, (rs, n) -> new NotificationRequest(
                        "appointment-" + appointmentId + "-" + type.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-'),
                        rs.getString("email"), type,
                        (type == NotificationType.BOOKING_CONFIRMATION ? "Booking confirmed: " : "Booking cancelled: ")
                                + rs.getString("service_name") + " at " + rs.getTimestamp("start_time").toLocalDateTime()),
                appointmentId);
    }

    public Optional<AppointmentState> findState(int appointmentId) {
        return jdbcTemplate.query(
                "SELECT user_id, status FROM appointments WHERE appointment_id = ?",
                (rs, rowNum) -> new AppointmentState(rs.getInt("user_id"), rs.getString("status")),
                appointmentId)
                .stream()
                .findFirst();
    }

    public int cancel(int appointmentId, int userId) {
        return jdbcTemplate.update("""
                UPDATE appointments
                SET status = 'CANCELLED'
                WHERE appointment_id = ? AND user_id = ? AND status = 'BOOKED'
                """, appointmentId, userId);
    }

    public void markPastAppointmentsCompleted() {
        jdbcTemplate.update("""
                UPDATE appointments SET status = 'COMPLETED'
                WHERE status = 'BOOKED' AND slot_id IN (
                    SELECT slot_id FROM availability_slots WHERE end_time < CURRENT_TIMESTAMP
                )
                """);
    }

    public List<AppointmentView> findForCustomer(int userId) {
        return jdbcTemplate.query(baseSelect() + " WHERE appointment.user_id = ? ORDER BY slot.start_time DESC",
                appointmentMapper(), userId);
    }

    public List<AppointmentView> findForProvider(int providerId) {
        return jdbcTemplate.query(baseSelect() + " WHERE slot.provider_id = ? ORDER BY slot.start_time DESC",
                appointmentMapper(), providerId);
    }

    private String baseSelect() {
        return """
                SELECT appointment.appointment_id,
                       customer.full_name AS customer_name,
                       provider_user.full_name AS provider_name,
                       service.service_name,
                       slot.start_time, slot.end_time, appointment.status
                FROM appointments appointment
                JOIN users customer ON customer.user_id = appointment.user_id
                JOIN availability_slots slot ON slot.slot_id = appointment.slot_id
                JOIN services service ON service.service_id = slot.service_id
                JOIN providers provider ON provider.provider_id = slot.provider_id
                JOIN users provider_user ON provider_user.user_id = provider.user_id
                """;
    }

    private RowMapper<AppointmentView> appointmentMapper() {
        return (rs, rowNum) -> new AppointmentView(
                rs.getInt("appointment_id"), rs.getString("customer_name"),
                rs.getString("provider_name"), rs.getString("service_name"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(), rs.getString("status"));
    }
}
