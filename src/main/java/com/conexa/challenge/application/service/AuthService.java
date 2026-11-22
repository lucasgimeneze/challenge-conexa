package com.conexa.challenge.application.service;

import com.conexa.challenge.domain.exception.InvalidCredentialsException;
import com.conexa.challenge.domain.model.AccessToken;
import com.conexa.challenge.domain.model.User;
import com.conexa.challenge.domain.port.out.PasswordHasher;
import com.conexa.challenge.domain.port.out.TokenIssuer;
import com.conexa.challenge.domain.port.out.UserRepository;
import java.util.UUID;

public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    public User register(String username, String rawPassword) {
        User user = new User(UUID.randomUUID(), username, passwordHasher.hash(rawPassword));
        return userRepository.save(user);
    }

    public AccessToken login(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordHasher.matches(rawPassword, user.passwordHash()))
                .map(tokenIssuer::issue)
                .orElseThrow(InvalidCredentialsException::new);
    }
}
