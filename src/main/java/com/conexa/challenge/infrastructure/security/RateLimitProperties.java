package com.conexa.challenge.infrastructure.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        @NotNull @Valid Limit auth,
        @NotNull @Valid Limit api) {

    public record Limit(
            @Min(1) int limit,
            @NotNull Duration period) {
    }
}
