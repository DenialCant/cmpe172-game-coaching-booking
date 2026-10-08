package com.cmpe172.game_coaching_booking.repository;

import java.util.List;
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

    public void create(int userId, int slotId) {
        jdbcTemplate.update(
                "INSERT INTO appointments (user_id, slot_id, status) VALUES (?, ?, 'BOOKED')",
                userId, slotId);
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
                UPDATE appointments appointment
                SET status = 'COMPLETED'
                FROM availability_slots slot
                WHERE appointment.slot_id = slot.slot_id
                  AND appointment.status = 'BOOKED'
                  AND slot.end_time < CURRENT_TIMESTAMP
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
