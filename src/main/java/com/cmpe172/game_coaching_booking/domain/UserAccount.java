package com.cmpe172.game_coaching_booking.domain;

public class UserAccount {

    private final int userId;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final String role;
    private final Integer providerId;

    public UserAccount(int userId, String username, String passwordHash,
            String fullName, String role, Integer providerId) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.providerId = providerId;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public Integer getProviderId() {
        return providerId;
    }
}
