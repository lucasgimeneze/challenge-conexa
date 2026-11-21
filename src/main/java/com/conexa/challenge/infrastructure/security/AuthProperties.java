package com.conexa.challenge.infrastructure.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(@Valid @NotNull Jwt jwt, @Valid @NotNull DemoUser demoUser) {

    // HS256 exige una clave de al menos 256 bits (32 bytes).
    public record Jwt(@NotBlank @Size(min = 32) String secret, @NotNull Duration expiration) {
    }

    public record DemoUser(boolean enabled, String username, String password) {
    }
}
