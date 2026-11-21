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

    /**
     * @param secret     clave HMAC; HS256 exige al menos 256 bits (32 bytes)
     * @param expiration vigencia del access token
     */
    public record Jwt(@NotBlank @Size(min = 32) String secret, @NotNull Duration expiration) {
    }

    /**
     * Usuario que se registra al arrancar, para poder probar la API sin registrarse
     * (los usuarios viven en memoria y se pierden al reiniciar).
     */
    public record DemoUser(boolean enabled, String username, String password) {
    }
}
