package com.conexa.challenge.infrastructure.adapter.out.swapi;

import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.INVALID_RESPONSE;
import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.RATE_LIMITED;
import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.TIMEOUT;
import static com.conexa.challenge.domain.exception.ExternalServiceException.Reason.UNAVAILABLE;

import com.conexa.challenge.domain.exception.ExternalServiceException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Component
public class SwapiErrorTranslator {

    public ExternalServiceException translate(RestClientException ex) {
        return switch (ex) {
            case HttpClientErrorException.TooManyRequests e -> new ExternalServiceException(RATE_LIMITED, retryAfter(e), e);
            case HttpServerErrorException e -> new ExternalServiceException(UNAVAILABLE, e);
            case RestClientException e when isTimeout(e) -> new ExternalServiceException(TIMEOUT, e);
            case ResourceAccessException e -> new ExternalServiceException(UNAVAILABLE, e);
            default -> new ExternalServiceException(INVALID_RESPONSE, ex);
        };
    }

    private static boolean isTimeout(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static Duration retryAfter(HttpClientErrorException ex) {
        HttpHeaders headers = ex.getResponseHeaders();
        String seconds = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (seconds == null || !seconds.matches("\\d+")) {
            return null;
        }
        return Duration.ofSeconds(Long.parseLong(seconds));
    }
}
