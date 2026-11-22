package com.conexa.challenge.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.conexa.challenge.domain.exception.InvalidCredentialsException;
import com.conexa.challenge.domain.exception.UserAlreadyExistsException;
import com.conexa.challenge.domain.model.AccessToken;
import com.conexa.challenge.domain.model.User;
import com.conexa.challenge.domain.port.out.TokenIssuer;
import com.conexa.challenge.infrastructure.adapter.out.persistence.InMemoryUserRepository;
import com.conexa.challenge.infrastructure.security.BCryptPasswordHasher;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final BCryptPasswordHasher passwordHasher = new BCryptPasswordHasher();
    private final TokenIssuer tokenIssuer = user -> new AccessToken("token-for-" + user.username(), 900);
    private final AuthService authService = new AuthService(userRepository, passwordHasher, tokenIssuer);

    @Test
    void registerAssignsIdAndStoresHashedPassword() {
        User user = authService.register("luke", "skywalker123");

        assertThat(user.id()).isNotNull();
        assertThat(user.passwordHash()).isNotEqualTo("skywalker123");
        assertThat(passwordHasher.matches("skywalker123", user.passwordHash())).isTrue();
        assertThat(userRepository.findByUsername("luke")).contains(user);
    }

    @Test
    void registerRejectsDuplicatedUsername() {
        authService.register("luke", "skywalker123");

        assertThatThrownBy(() -> authService.register("luke", "another-pass"))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void loginIssuesTokenForTheUserWithValidCredentials() {
        authService.register("luke", "skywalker123");

        AccessToken token = authService.login("luke", "skywalker123");

        assertThat(token).isEqualTo(new AccessToken("token-for-luke", 900));
    }

    @Test
    void loginRejectsWrongPassword() {
        authService.register("luke", "skywalker123");

        assertThatThrownBy(() -> authService.login("luke", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsUnknownUserWithSameError() {
        assertThatThrownBy(() -> authService.login("vader", "whatever123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }
}
