package com.conexa.challenge.infrastructure.adapter.out.swapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class SwapiFixtures {

    private SwapiFixtures() {
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
