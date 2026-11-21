package com.conexa.challenge.infrastructure.security;

import com.conexa.challenge.domain.exception.UserAlreadyExistsException;
import com.conexa.challenge.domain.model.User;
import com.conexa.challenge.domain.port.out.UserRepository;
import com.conexa.challenge.infrastructure.security.JwtTokenProvider.AccessToken;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * @throws UserAlreadyExistsException si el username ya está registrado
     */
    public User register(String username, String rawPassword) {
        User user = new User(UUID.randomUUID(), username, passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    /**
     * @throws InvalidCredentialsException si el usuario no existe o la contraseña no coincide
     */
    public AccessToken login(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(rawPassword, user.passwordHash()))
                .map(tokenProvider::issue)
                .orElseThrow(InvalidCredentialsException::new);
    }
}
