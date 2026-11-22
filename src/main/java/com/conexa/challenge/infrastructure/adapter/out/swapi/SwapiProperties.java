package com.conexa.challenge.infrastructure.adapter.out.swapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "swapi")
public record SwapiProperties(
        @NotBlank String baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @NotNull @Valid RetryProperties retry,
        @NotNull @Valid CircuitBreakerProperties circuitBreaker) {

    public record RetryProperties(
            @Min(1) int maxAttempts,
            @NotNull Duration initialBackoff) {
    }

    public record CircuitBreakerProperties(
            @Min(1) int slidingWindowSize,
            @Min(1) int minimumCalls,
            @Min(1) @Max(100) int failureRateThreshold,
            @NotNull Duration openDuration) {
    }
}
