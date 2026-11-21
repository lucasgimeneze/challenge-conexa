package com.conexa.challenge.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void usesConfiguredSecretAsSigningKey() {
        String secret = "configured-secret-0123456789abcdef0123";

        SecretKey key = securityConfig.jwtSigningKey(properties(secret));

        assertThat(key.getEncoded()).isEqualTo(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void generatesRandom256BitKeyWhenSecretIsNotSet() {
        SecretKey first = securityConfig.jwtSigningKey(properties(""));
        SecretKey second = securityConfig.jwtSigningKey(properties(null));

        assertThat(first.getEncoded()).hasSize(32);
        assertThat(second.getEncoded()).hasSize(32);
        assertThat(first.getEncoded()).isNotEqualTo(second.getEncoded());
    }

    private static AuthProperties properties(String secret) {
        return new AuthProperties(new AuthProperties.Jwt(secret, Duration.ofHours(1)));
    }
}
