package com.conexa.challenge.infrastructure.config;

import com.conexa.challenge.application.service.AuthService;
import com.conexa.challenge.domain.port.out.PasswordHasher;
import com.conexa.challenge.domain.port.out.TokenIssuer;
import com.conexa.challenge.domain.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

    @Bean
    AuthService authService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        return new AuthService(userRepository, passwordHasher, tokenIssuer);
    }
}
