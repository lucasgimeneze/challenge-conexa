package com.conexa.challenge.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.conexa.challenge.domain.exception.UserAlreadyExistsException;
import com.conexa.challenge.domain.model.User;
import com.conexa.challenge.infrastructure.adapter.out.persistence.InMemoryUserRepository;
import com.conexa.challenge.infrastructure.security.JwtTokenProvider.AccessToken;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private static final AuthProperties PROPERTIES = new AuthProperties(
            new AuthProperties.Jwt("test-secret-0123456789abcdef0123456789", Duration.ofMinutes(15)),
            new AuthProperties.DemoUser(false, null, null));

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenProvider tokenProvider =
            new JwtTokenProvider(new SecurityConfig().jwtEncoder(PROPERTIES), PROPERTIES, Clock.systemUTC());
    private final AuthService authService = new AuthService(userRepository, passwordEncoder, tokenProvider);

    @Test
    void registerAssignsIdAndStoresHashedPassword() {
        User user = authService.register("luke", "skywalker123");

        assertThat(user.id()).isNotNull();
        assertThat(user.passwordHash()).isNotEqualTo("skywalker123");
        assertThat(passwordEncoder.matches("skywalker123", user.passwordHash())).isTrue();
        assertThat(userRepository.findByUsername("luke")).contains(user);
    }

    @Test
    void registerRejectsDuplicatedUsername() {
        authService.register("luke", "skywalker123");

        assertThatThrownBy(() -> authService.register("luke", "another-pass"))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        authService.register("luke", "skywalker123");

        AccessToken token = authService.login("luke", "skywalker123");

        assertThat(token.value()).isNotBlank();
        assertThat(token.expiresInSeconds()).isEqualTo(900);
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
