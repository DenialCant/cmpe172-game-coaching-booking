package com.cmpe172.game_coaching_booking.service;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.cmpe172.game_coaching_booking.domain.UserAccount;
import com.cmpe172.game_coaching_booking.repository.AuthRepository;

@Service
public class AuthService {

    private final AuthRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AuthRepository repository) {
        this.repository = repository;
    }

    public Optional<UserAccount> authenticate(String username, String password) {
        return repository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()));
    }
}
