package com.conexa.challenge.infrastructure.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(@Valid @NotNull Jwt jwt) {

    /**
     * @param secret opcional: sin valor se usa una clave aleatoria efímera (ver {@code SecurityConfig}).
     *               Si se define, HS256 exige al menos 256 bits (32 bytes).
     */
    public record Jwt(@Size(min = 32) String secret, @NotNull Duration expiration) {

        public Jwt {
            if (secret != null && secret.isBlank()) {
                secret = null;
            }
        }

        public boolean hasSecret() {
            return secret != null;
        }
    }
}
