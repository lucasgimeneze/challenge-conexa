package com.conexa.challenge.infrastructure.adapter.in.web;

import static com.conexa.challenge.infrastructure.adapter.out.swapi.SwapiFixtures.json;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conexa.challenge.domain.model.User;
import com.conexa.challenge.infrastructure.security.JwtTokenProvider;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "swapi.retry.max-attempts=1",
        "swapi.circuit-breaker.sliding-window-size=3",
        "swapi.circuit-breaker.minimum-calls=3",
        "swapi.circuit-breaker.open-duration=30s"
})
class CacheAndCircuitIntegrationTest {

    @RegisterExtension
    static final WireMockExtension swapi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void swapiBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("swapi.base-url", () -> swapi.baseUrl() + "/api");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void cachedResponsesAreServedWhileTheCircuitIsOpen() throws Exception {
        swapi.stubFor(WireMock.get(urlPathEqualTo("/api/people"))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("limit", equalTo("2"))
                .willReturn(json("people-page.json")));
        perform("/api/v1/people?size=2").andExpect(status().isOk());

        swapi.stubFor(WireMock.get(urlPathEqualTo("/api/films")).willReturn(serverError()));
        perform("/api/v1/films").andExpect(status().isBadGateway());
        perform("/api/v1/films").andExpect(status().isBadGateway());

        perform("/api/v1/films")
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"));
        perform("/api/v1/people?size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Luke Skywalker"));
    }

    private ResultActions perform(String uri) throws Exception {
        String token = tokenProvider.issue(new User(UUID.randomUUID(), "luke", "hash")).value();
        return mockMvc.perform(get(uri).header("Authorization", "Bearer " + token));
    }
}
