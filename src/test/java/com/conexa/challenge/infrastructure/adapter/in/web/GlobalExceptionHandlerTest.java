package com.conexa.challenge.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.conexa.challenge.domain.exception.ExternalServiceException;
import com.conexa.challenge.domain.exception.ExternalServiceException.Reason;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "TIMEOUT,          504",
            "UNAVAILABLE,      502",
            "INVALID_RESPONSE, 502",
            "RATE_LIMITED,     503",
            "CIRCUIT_OPEN,     503"
    })
    void mapsEachSwapiFailureToItsStatus(Reason reason, int expectedStatus) {
        ResponseEntity<ProblemDetail> response = handler.handleExternalService(new ExternalServiceException(reason));

        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        assertThat(response.getBody().getDetail()).isEqualTo(new ExternalServiceException(reason).getMessage());
    }

    @ParameterizedTest
    @CsvSource({"RATE_LIMITED", "CIRCUIT_OPEN"})
    void propagatesRetryAfterInSeconds(Reason reason) {
        ResponseEntity<ProblemDetail> response = handler.handleExternalService(
                new ExternalServiceException(reason, Duration.ofSeconds(30), null));

        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("30");
    }
}
