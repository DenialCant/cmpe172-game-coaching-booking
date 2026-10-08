package com.cmpe172.game_coaching_booking.repository;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.cmpe172.game_coaching_booking.domain.UserAccount;

@Repository
public class AuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserAccount> findByUsername(String username) {
        String sql = """
                SELECT u.user_id, u.username, u.password_hash, u.full_name,
                       u.role, p.provider_id
                FROM users u
                LEFT JOIN providers p ON p.user_id = u.user_id
                WHERE LOWER(u.username) = LOWER(?)
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new UserAccount(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        (Integer) rs.getObject("provider_id")),
                username.trim())
                .stream()
                .findFirst();
    }
}
