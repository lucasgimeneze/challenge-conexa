package com.conexa.challenge.infrastructure.adapter.out.swapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class SwapiFixtures {

    private SwapiFixtures() {
    }

    public static SwapiProperties properties(String baseUrl) {
        return properties(baseUrl, new SwapiProperties.CircuitBreaker(100, 100, 50, Duration.ofSeconds(1)));
    }

    public static SwapiProperties properties(String baseUrl, SwapiProperties.CircuitBreaker circuitBreaker) {
        return new SwapiProperties(baseUrl, Duration.ofSeconds(1), Duration.ofMillis(500),
                new SwapiProperties.Retry(3, Duration.ofMillis(10)), circuitBreaker);
    }

    public static SwapiClient client(String baseUrl) {
        return new SwapiClient(properties(baseUrl), new SwapiErrorTranslator());
    }

    public static ResponseDefinitionBuilder json(String fixture) {
        return json(200, fixture);
    }

    public static ResponseDefinitionBuilder json(int status, String fixture) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody(read(fixture));
    }

    private static String read(String fixture) {
        try (InputStream input = SwapiFixtures.class.getResourceAsStream("/swapi/" + fixture)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
