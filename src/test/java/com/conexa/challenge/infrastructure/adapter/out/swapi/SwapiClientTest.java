package com.conexa.challenge.infrastructure.adapter.out.swapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.conexa.challenge.domain.exception.ExternalServiceException;
import com.conexa.challenge.domain.exception.ExternalServiceException.Reason;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiList;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiPersonProperties;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.ParameterizedTypeReference;

class SwapiClientTest {

    private static final ParameterizedTypeReference<SwapiList<SwapiPersonProperties>> PEOPLE =
            new ParameterizedTypeReference<>() {
            };

    @RegisterExtension
    static final WireMockExtension swapi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final SwapiClient client = new SwapiClient(
            new SwapiProperties(swapi.baseUrl(), Duration.ofSeconds(1), Duration.ofMillis(500)),
            new SwapiErrorTranslator());

    @Test
    void slowResponseIsTimeout() {
        swapi.stubFor(get("/people").willReturn(okJson("{\"result\": []}").withFixedDelay(1_500)));

        assertFailsWith(Reason.TIMEOUT, this::fetchPeople);
    }

    @Test
    void droppedConnectionIsUnavailable() {
        swapi.stubFor(get("/people").willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertFailsWith(Reason.UNAVAILABLE, this::fetchPeople);
    }

    @Test
    void serverErrorIsUnavailable() {
        swapi.stubFor(get("/people").willReturn(serverError()));

        assertFailsWith(Reason.UNAVAILABLE, this::fetchPeople);
    }

    @Test
    void tooManyRequestsIsRateLimitedWithRetryAfter() {
        swapi.stubFor(get("/people").willReturn(aResponse().withStatus(429).withHeader("Retry-After", "60")));

        assertThatThrownBy(this::fetchPeople)
                .isInstanceOfSatisfying(ExternalServiceException.class, ex -> {
                    assertThat(ex.reason()).isEqualTo(Reason.RATE_LIMITED);
                    assertThat(ex.retryAfter()).contains(Duration.ofSeconds(60));
                });
    }

    @Test
    void htmlInsteadOfJsonIsInvalidResponse() {
        swapi.stubFor(get("/people").willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody("<html><body>Under maintenance</body></html>")));

        assertFailsWith(Reason.INVALID_RESPONSE, this::fetchPeople);
    }

    @Test
    void envelopeWithoutResultIsInvalidResponse() {
        swapi.stubFor(get("/people").willReturn(okJson("{\"message\": \"ok\"}")));

        assertFailsWith(Reason.INVALID_RESPONSE, this::fetchPeople);
    }

    @Test
    void itemWithoutNameIsInvalidResponse() {
        swapi.stubFor(get("/people").willReturn(okJson("""
                {"result": [{"uid": "1", "properties": {"gender": "male"}}]}
                """)));

        assertFailsWith(Reason.INVALID_RESPONSE, this::fetchPeople);
    }

    @Test
    void emptyBodyIsInvalidResponseEvenWhenTheResourceIsOptional() {
        swapi.stubFor(get("/people").willReturn(aResponse().withStatus(200)));

        assertFailsWith(Reason.INVALID_RESPONSE, () -> client.find(PEOPLE, "/people"));
    }

    @Test
    void notFoundIsEmptyWhenTheResourceIsOptional() {
        swapi.stubFor(get("/people").willReturn(aResponse().withStatus(404)));

        assertThat(client.find(PEOPLE, "/people")).isEmpty();
    }

    @Test
    void notFoundIsInvalidResponseWhenTheResourceIsRequired() {
        swapi.stubFor(get("/people").willReturn(aResponse().withStatus(404)));

        assertFailsWith(Reason.INVALID_RESPONSE, this::fetchPeople);
    }

    private void fetchPeople() {
        client.fetch(PEOPLE, "/people");
    }

    private static void assertFailsWith(Reason reason, ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(ExternalServiceException.class, ex -> assertThat(ex.reason()).isEqualTo(reason));
    }
}
