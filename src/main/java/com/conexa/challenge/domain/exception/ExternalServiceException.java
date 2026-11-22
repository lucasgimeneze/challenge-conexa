package com.conexa.challenge.domain.exception;

import java.time.Duration;
import java.util.Optional;

public class ExternalServiceException extends RuntimeException {

    public enum Reason {
        TIMEOUT("The Star Wars API did not respond in time"),
        UNAVAILABLE("The Star Wars API is unavailable"),
        RATE_LIMITED("The Star Wars API rate limit was exceeded"),
        CIRCUIT_OPEN("The Star Wars API is failing repeatedly, try again later"),
        INVALID_RESPONSE("The Star Wars API returned an invalid response");

        private final String message;

        Reason(String message) {
            this.message = message;
        }
    }

    private final Reason reason;
    private final Duration retryAfter;

    public ExternalServiceException(Reason reason) {
        this(reason, null, null);
    }

    public ExternalServiceException(Reason reason, Throwable cause) {
        this(reason, null, cause);
    }

    public ExternalServiceException(Reason reason, Duration retryAfter, Throwable cause) {
        super(reason.message, cause);
        this.reason = reason;
        this.retryAfter = retryAfter;
    }

    public Reason reason() {
        return reason;
    }

    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}
