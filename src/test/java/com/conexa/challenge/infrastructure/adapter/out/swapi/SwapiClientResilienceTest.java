package com.conexa.challenge.infrastructure.adapter.out.swapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.conexa.challenge.domain.exception.ExternalServiceException;
import com.conexa.challenge.domain.exception.ExternalServiceException.Reason;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiList;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiPersonProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.ParameterizedTypeReference;

class SwapiClientResilienceTest {

    private static final ParameterizedTypeReference<SwapiList<SwapiPersonProperties>> PEOPLE =
            new ParameterizedTypeReference<>() {
            };
    private static final String EMPTY_RESULT = "{\"result\": []}";
    private static final Duration OPEN_DURATION = Duration.ofMillis(300);

    @RegisterExtension
    static final WireMockExtension swapi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final SwapiClient client = SwapiFixtures.client(swapi.baseUrl());

    @Test
    void retriesTransientFailureUntilItRecovers() {
        swapi.stubFor(get("/people").inScenario("flaky").whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(503)).willSetStateTo("recovered"));
        swapi.stubFor(get("/people").inScenario("flaky").whenScenarioStateIs("recovered")
                .willReturn(okJson(EMPTY_RESULT)));

        assertThat(client.fetch(PEOPLE, "/people").result()).isEmpty();
        swapi.verify(2, getRequestedFor(urlEqualTo("/people")));
    }

    @Test
    void givesUpAfterMaxAttempts() {
        swapi.stubFor(get("/people").willReturn(serverError()));

        assertFailsWith(Reason.UNAVAILABLE, () -> client.fetch(PEOPLE, "/people"));
        swapi.verify(3, getRequestedFor(urlEqualTo("/people")));
    }

    @Test
    void doesNotRetryTimeouts() {
        swapi.stubFor(get("/people").willReturn(okJson(EMPTY_RESULT).withFixedDelay(1_000)));

        assertFailsWith(Reason.TIMEOUT, () -> client.fetch(PEOPLE, "/people"));
        swapi.verify(1, getRequestedFor(urlEqualTo("/people")));
    }

    @Test
    void doesNotRetryRateLimits() {
        swapi.stubFor(get("/people").willReturn(aResponse().withStatus(429)));

        assertFailsWith(Reason.RATE_LIMITED, () -> client.fetch(PEOPLE, "/people"));
        swapi.verify(1, getRequestedFor(urlEqualTo("/people")));
    }

    @Test
    void doesNotRetryInvalidResponses() {
        swapi.stubFor(get("/people").willReturn(okJson("{\"message\": \"ok\"}")));

        assertFailsWith(Reason.INVALID_RESPONSE, () -> client.fetch(PEOPLE, "/people"));
        swapi.verify(1, getRequestedFor(urlEqualTo("/people")));
    }

    @Test
    void opensTheCircuitAfterRepeatedFailuresAndStopsCallingSwapi() {
        SwapiClient client = clientWithCircuitOf(4);
        swapi.stubFor(get("/people").willReturn(serverError()));
        for (int i = 0; i < 4; i++) {
            assertFailsWith(Reason.UNAVAILABLE, () -> client.fetch(PEOPLE, "/people"));
        }
        swapi.resetRequests();

        assertThatThrownBy(() -> client.fetch(PEOPLE, "/people"))
                .isInstanceOfSatisfying(ExternalServiceException.class, ex -> {
                    assertThat(ex.reason()).isEqualTo(Reason.CIRCUIT_OPEN);
                    assertThat(ex.retryAfter()).contains(OPEN_DURATION);
                });
        swapi.verify(0, getRequestedFor(urlEqualTo("/people")));
    }

    @Test
    void closesTheCircuitWhenSwapiRecovers() throws InterruptedException {
        SwapiClient client = clientWithCircuitOf(4);
        swapi.stubFor(get("/people").willReturn(serverError()));
        for (int i = 0; i < 4; i++) {
            assertFailsWith(Reason.UNAVAILABLE, () -> client.fetch(PEOPLE, "/people"));
        }

        swapi.stubFor(get("/people").willReturn(okJson(EMPTY_RESULT)));
        Thread.sleep(OPEN_DURATION.plusMillis(100));

        assertThat(client.fetch(PEOPLE, "/people").result()).isEmpty();
    }

    @Test
    void notFoundDoesNotCountAsFailure() {
        SwapiClient client = clientWithCircuitOf(2);
        swapi.stubFor(get("/people").willReturn(aResponse().withStatus(404)));

        for (int i = 0; i < 5; i++) {
            assertThat(client.find(PEOPLE, "/people")).isEmpty();
        }
    }

    private static SwapiClient clientWithCircuitOf(int windowSize) {
        SwapiProperties.CircuitBreaker circuit = new SwapiProperties.CircuitBreaker(windowSize, windowSize, 50, OPEN_DURATION);
        return new SwapiClient(SwapiFixtures.properties(swapi.baseUrl(), circuit), new SwapiErrorTranslator());
    }

    private static void assertFailsWith(Reason reason, Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ExternalServiceException.class, ex -> assertThat(ex.reason()).isEqualTo(reason));
    }
}
