package com.conexa.challenge.infrastructure.adapter.out.swapi;

import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.CIRCUIT_OPEN;
import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.INVALID_RESPONSE;
import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.RATE_LIMITED;
import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.TIMEOUT;
import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.UNAVAILABLE;

import com.conexa.challenge.domain.exception.ExternalServiceException;
import com.conexa.challenge.domain.exception.ExternalServiceException.Reason;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Circuit breaker por fuera del retry: una request con sus reintentos cuenta como una sola llamada para el circuito.
 * Un 429 o una respuesta inválida se ignoran (no son caída ni éxito): para Resilience4j, lo que no se registra
 * como falla cuenta como éxito y bajaría el porcentaje de fallas.
 */
class SwapiResilience {

    private static final Logger log = LoggerFactory.getLogger(SwapiResilience.class);

    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final double BACKOFF_RANDOMIZATION = 0.5;
    private static final int CALLS_IN_HALF_OPEN_STATE = 3;

    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final Duration openDuration;

    SwapiResilience(SwapiProperties.RetryProperties retryProperties,
            SwapiProperties.CircuitBreakerProperties circuitProperties) {
        this.retry = Retry.of("swapi", RetryConfig.custom()
                .maxAttempts(retryProperties.maxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                        retryProperties.initialBackoff(), BACKOFF_MULTIPLIER, BACKOFF_RANDOMIZATION))
                .retryOnException(failure -> hasReason(failure, Set.of(UNAVAILABLE)))
                .build());
        this.circuitBreaker = CircuitBreaker.of("swapi", CircuitBreakerConfig.custom()
                .slidingWindowSize(circuitProperties.slidingWindowSize())
                .minimumNumberOfCalls(circuitProperties.minimumCalls())
                .failureRateThreshold(circuitProperties.failureRateThreshold())
                .waitDurationInOpenState(circuitProperties.openDuration())
                .permittedNumberOfCallsInHalfOpenState(CALLS_IN_HALF_OPEN_STATE)
                .recordException(failure -> hasReason(failure, Set.of(UNAVAILABLE, TIMEOUT)))
                .ignoreException(failure -> hasReason(failure, Set.of(RATE_LIMITED, INVALID_RESPONSE)))
                .build());
        this.circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("SWAPI circuit breaker: {}", event.getStateTransition()));
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

    private static boolean hasReason(Throwable failure, Set<Reason> reasons) {
        return failure instanceof ExternalServiceException ex && reasons.contains(ex.reason());
    }
}
