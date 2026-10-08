package com.cmpe172.game_coaching_booking.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HomeRepository {

    private final JdbcTemplate jdbcTemplate;

    public HomeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countProviders() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM providers",
                Integer.class
        );

        return count != null ? count : 0;
    }

    public int countServices() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM services",
                Integer.class
        );

        return count != null ? count : 0;
    }

    public int countAvailableSlots() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM availability_slots slot
                WHERE slot.active = TRUE
                  AND slot.start_time > CURRENT_TIMESTAMP
                  AND NOT EXISTS (
                      SELECT 1 FROM appointments appointment
                      WHERE appointment.slot_id = slot.slot_id
                        AND appointment.status = 'BOOKED'
                  )
                """,
                Integer.class
        );

        return count != null ? count : 0;
    }
}
