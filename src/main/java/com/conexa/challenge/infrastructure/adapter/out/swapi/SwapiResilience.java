package com.conexa.challenge.infrastructure.adapter.out.swapi;

import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.CIRCUIT_OPEN;
import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.TIMEOUT;
import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.UNAVAILABLE;

import com.conexa.challenge.domain.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Circuit breaker por fuera del retry: una request con sus reintentos cuenta como una sola llamada para el circuito.
 */
class SwapiResilience {

    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final double BACKOFF_RANDOMIZATION = 0.5;
    private static final int CALLS_IN_HALF_OPEN_STATE = 3;

    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final Duration openDuration;

    SwapiResilience(SwapiProperties.Retry retryProperties, SwapiProperties.CircuitBreaker circuitProperties) {
        this.retry = Retry.of("swapi", RetryConfig.custom()
                .maxAttempts(retryProperties.maxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                        retryProperties.initialBackoff(), BACKOFF_MULTIPLIER, BACKOFF_RANDOMIZATION))
                .retryOnException(SwapiResilience::isTransient)
                .build());
        this.circuitBreaker = CircuitBreaker.of("swapi", CircuitBreakerConfig.custom()
                .slidingWindowSize(circuitProperties.slidingWindowSize())
                .minimumNumberOfCalls(circuitProperties.minimumCalls())
                .failureRateThreshold(circuitProperties.failureRateThreshold())
                .waitDurationInOpenState(circuitProperties.openDuration())
                .permittedNumberOfCallsInHalfOpenState(CALLS_IN_HALF_OPEN_STATE)
                .recordException(SwapiResilience::isOutage)
                .build());
        this.openDuration = circuitProperties.openDuration();
    }

    <T> T execute(Supplier<T> call) {
        Supplier<T> withRetry = Retry.decorateSupplier(retry, call);
        try {
            return CircuitBreaker.decorateSupplier(circuitBreaker, withRetry).get();
        } catch (CallNotPermittedException ex) {
            throw new ExternalServiceException(CIRCUIT_OPEN, openDuration, ex);
        }
    }

    private static boolean isTransient(Throwable failure) {
        return failure instanceof ExternalServiceException ex && ex.reason() == UNAVAILABLE;
    }

    private static boolean isOutage(Throwable failure) {
        return failure instanceof ExternalServiceException ex
                && (ex.reason() == UNAVAILABLE || ex.reason() == TIMEOUT);
    }
}
