package com.cmpe172.game_coaching_booking.repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.cmpe172.game_coaching_booking.domain.AvailabilitySlot;
import com.cmpe172.game_coaching_booking.dto.AvailableSlotView;
import com.cmpe172.game_coaching_booking.dto.ProviderOption;
import com.cmpe172.game_coaching_booking.dto.ServiceOption;

@Repository
public class AvailabilitySlotRepository {

    private static final String AVAILABLE_FROM = """
            FROM availability_slots slot
            JOIN providers provider ON provider.provider_id = slot.provider_id
            JOIN users provider_user ON provider_user.user_id = provider.user_id
            JOIN services service ON service.service_id = slot.service_id
            WHERE slot.active = TRUE
              AND slot.start_time > CURRENT_TIMESTAMP
              AND NOT EXISTS (
                    SELECT 1 FROM appointments appointment
                    WHERE appointment.slot_id = slot.slot_id
                      AND appointment.status = 'BOOKED'
              )
            """;

    private final JdbcTemplate jdbcTemplate;

    public AvailabilitySlotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AvailableSlotView> findAvailable(Integer providerId, Integer serviceId,
            LocalDate date, int limit, int offset) {
        List<Object> parameters = new ArrayList<>();
        String filters = buildFilters(providerId, serviceId, date, parameters);
        String sql = """
                SELECT slot.slot_id, slot.provider_id, provider_user.full_name AS provider_name,
                       slot.service_id, service.service_name, service.duration_minutes,
                       slot.start_time, slot.end_time
                """ + AVAILABLE_FROM + filters + " ORDER BY slot.start_time LIMIT ? OFFSET ?";
        parameters.add(limit);
        parameters.add(offset);

        return jdbcTemplate.query(sql, (rs, rowNum) -> new AvailableSlotView(
                rs.getInt("slot_id"),
                rs.getInt("provider_id"),
                rs.getString("provider_name"),
                rs.getInt("service_id"),
                rs.getString("service_name"),
                rs.getInt("duration_minutes"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime()),
                parameters.toArray());
    }

    public long countAvailable(Integer providerId, Integer serviceId, LocalDate date) {
        List<Object> parameters = new ArrayList<>();
        String filters = buildFilters(providerId, serviceId, date, parameters);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + AVAILABLE_FROM + filters,
                Long.class,
                parameters.toArray());
        return count == null ? 0 : count;
    }

    private String buildFilters(Integer providerId, Integer serviceId, LocalDate date,
            List<Object> parameters) {
        StringBuilder filters = new StringBuilder();
        if (providerId != null) {
            filters.append(" AND slot.provider_id = ?");
            parameters.add(providerId);
        }
        if (serviceId != null) {
            filters.append(" AND slot.service_id = ?");
            parameters.add(serviceId);
        }
        if (date != null) {
            filters.append(" AND slot.start_time >= ? AND slot.start_time < ?");
            parameters.add(Timestamp.valueOf(date.atStartOfDay()));
            parameters.add(Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
        }
        return filters.toString();
    }

    public Optional<AvailabilitySlot> findByIdForUpdate(int slotId) {
        String sql = """
                SELECT slot_id, provider_id, service_id, start_time, end_time
                FROM availability_slots
                WHERE slot_id = ? AND active = TRUE
                FOR UPDATE
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AvailabilitySlot(
                rs.getInt("slot_id"),
                rs.getInt("provider_id"),
                rs.getInt("service_id"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime()), slotId)
                .stream()
                .findFirst();
    }

    public List<ServiceOption> findServicesForProvider(int providerId) {
        return jdbcTemplate.query(
                "SELECT service_id, service_name FROM services WHERE provider_id = ? ORDER BY service_name",
                (rs, rowNum) -> new ServiceOption(rs.getInt("service_id"), rs.getString("service_name")),
                providerId);
    }

    public List<ServiceOption> findAllServices() {
        return jdbcTemplate.query(
                "SELECT service_id, service_name FROM services ORDER BY service_name",
                (rs, rowNum) -> new ServiceOption(rs.getInt("service_id"), rs.getString("service_name")));
    }

    public List<ProviderOption> findAllProviders() {
        return jdbcTemplate.query("""
                SELECT provider.provider_id, users.full_name
                FROM providers provider
                JOIN users ON users.user_id = provider.user_id
                ORDER BY users.full_name
                """,
                (rs, rowNum) -> new ProviderOption(rs.getInt("provider_id"), rs.getString("full_name")));
    }

    public boolean serviceBelongsToProvider(int serviceId, int providerId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM services WHERE service_id = ? AND provider_id = ?",
                Integer.class, serviceId, providerId);
        return count != null && count == 1;
    }

    public void create(int providerId, int serviceId, LocalDateTime startTime, LocalDateTime endTime) {
        jdbcTemplate.update("""
                INSERT INTO availability_slots
                    (provider_id, service_id, start_time, end_time, active)
                VALUES (?, ?, ?, ?, TRUE)
                """, providerId, serviceId, Timestamp.valueOf(startTime), Timestamp.valueOf(endTime));
    }

    public boolean hasOverlap(int providerId, LocalDateTime startTime, LocalDateTime endTime) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM availability_slots
                WHERE provider_id = ? AND active = TRUE
                  AND start_time < ? AND end_time > ?
                """, Integer.class, providerId, Timestamp.valueOf(endTime), Timestamp.valueOf(startTime));
        return count != null && count > 0;
    }

    public List<AvailableSlotView> findForProvider(int providerId) {
        return jdbcTemplate.query("""
                SELECT slot.slot_id, slot.provider_id, users.full_name AS provider_name,
                       slot.service_id, service.service_name, service.duration_minutes,
                       slot.start_time, slot.end_time
                FROM availability_slots slot
                JOIN providers provider ON provider.provider_id = slot.provider_id
                JOIN users ON users.user_id = provider.user_id
                JOIN services service ON service.service_id = slot.service_id
                WHERE slot.provider_id = ? AND slot.active = TRUE
                ORDER BY slot.start_time
                """,
                (rs, rowNum) -> new AvailableSlotView(
                        rs.getInt("slot_id"), rs.getInt("provider_id"), rs.getString("provider_name"),
                        rs.getInt("service_id"), rs.getString("service_name"),
                        rs.getInt("duration_minutes"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime()),
                providerId);
    }

    public boolean existsOwnedSlot(int slotId, int providerId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM availability_slots WHERE slot_id = ? AND provider_id = ? AND active = TRUE",
                Integer.class, slotId, providerId);
        return count != null && count == 1;
    }

    public int deactivate(int slotId, int providerId) {
        return jdbcTemplate.update("""
                UPDATE availability_slots
                SET active = FALSE
                WHERE slot_id = ? AND provider_id = ? AND active = TRUE
                  AND NOT EXISTS (
                      SELECT 1 FROM appointments
                      WHERE appointments.slot_id = availability_slots.slot_id
                        AND appointments.status = 'BOOKED'
                  )
                """, slotId, providerId);
    }
}
