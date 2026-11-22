package com.conexa.challenge.infrastructure.adapter.out.swapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.conexa.challenge.domain.exception.ExternalServiceException;
import com.conexa.challenge.domain.exception.ExternalServiceException.Reason;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

class SwapiErrorTranslatorTest {

    private final SwapiErrorTranslator translator = new SwapiErrorTranslator();

    @Test
    void timeoutBecomesTimeout() {
        ExternalServiceException result = translator.translate(
                new ResourceAccessException("I/O error", new HttpTimeoutException("request timed out")));

        assertThat(result.reason()).isEqualTo(Reason.TIMEOUT);
    }

    @Test
    void connectionFailureBecomesUnavailable() {
        ExternalServiceException result = translator.translate(
                new ResourceAccessException("I/O error", new ConnectException("Connection refused")));

        assertThat(result.reason()).isEqualTo(Reason.UNAVAILABLE);
    }

    @Test
    void serverErrorBecomesUnavailable() {
        ExternalServiceException result = translator.translate(
                HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Unavailable", null, null, null));

        assertThat(result.reason()).isEqualTo(Reason.UNAVAILABLE);
    }

    @Test
    void tooManyRequestsBecomesRateLimitedWithRetryAfter() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "30");

        ExternalServiceException result = translator.translate(tooManyRequests(headers));

        assertThat(result.reason()).isEqualTo(Reason.RATE_LIMITED);
        assertThat(result.retryAfter()).contains(Duration.ofSeconds(30));
    }

    @Test
    void retryAfterAsHttpDateIsIgnored() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "Wed, 21 Oct 2026 07:28:00 GMT");

        ExternalServiceException result = translator.translate(tooManyRequests(headers));

        assertThat(result.reason()).isEqualTo(Reason.RATE_LIMITED);
        assertThat(result.retryAfter()).isEmpty();
    }

    @Test
    void tooManyRequestsWithoutRetryAfterHasNoRetryAfter() {
        ExternalServiceException result = translator.translate(tooManyRequests(new HttpHeaders()));

        assertThat(result.reason()).isEqualTo(Reason.RATE_LIMITED);
        assertThat(result.retryAfter()).isEmpty();
    }

    @Test
    void timeoutWhileReadingTheBodyBecomesTimeout() {
        ExternalServiceException result = translator.translate(new RestClientException(
                "Error while extracting response", new UncheckedIOException(
                        new HttpTimeoutException("request timed out"))));

        assertThat(result.reason()).isEqualTo(Reason.TIMEOUT);
    }

    @Test
    void unexpectedClientErrorBecomesInvalidResponse() {
        ExternalServiceException result = translator.translate(
                HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null, null, null));

        assertThat(result.reason()).isEqualTo(Reason.INVALID_RESPONSE);
    }

    @Test
    void unreadableBodyBecomesInvalidResponse() {
        ExternalServiceException result = translator.translate(
                new RestClientException("Error while extracting response"));

        assertThat(result.reason()).isEqualTo(Reason.INVALID_RESPONSE);
    }

    private static HttpClientErrorException tooManyRequests(HttpHeaders headers) {
        return HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, new byte[0], StandardCharsets.UTF_8);
    }
}
