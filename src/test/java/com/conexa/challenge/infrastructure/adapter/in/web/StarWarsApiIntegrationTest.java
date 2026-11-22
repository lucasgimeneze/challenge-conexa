package com.conexa.challenge.infrastructure.adapter.in.web;

import static com.conexa.challenge.infrastructure.adapter.out.swapi.SwapiFixtures.json;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conexa.challenge.domain.model.User;
import com.conexa.challenge.infrastructure.security.JwtTokenProvider;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StarWarsApiIntegrationTest {

    private static final int RETRY_MAX_ATTEMPTS = 3;

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

    @Autowired
    private CacheManager cacheManager;

    private String token;

    @BeforeEach
    void setUp() {
        cacheManager.getCache("swapi").clear();
        token = tokenProvider.issue(new User(UUID.randomUUID(), "luke", "hash")).value();
    }

    @Test
    void listsPeopleWithCamelCaseFieldsAndPagination() throws Exception {
        stubPeoplePage();

        perform("/api/v1/people?size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Luke Skywalker"))
                .andExpect(jsonPath("$.content[0].birthYear").value("19BBY"))
                .andExpect(jsonPath("$.content[0].birth_year").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(82))
                .andExpect(jsonPath("$.totalPages").value(41));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "starships, starships-page.json, CR90 corvette",
            "vehicles,  vehicles-page.json,  Sand Crawler"
    })
    void listsStarshipsAndVehicles(String resource, String fixture, String firstName) throws Exception {
        stubPage(resource, fixture);

        perform("/api/v1/" + resource + "?size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(firstName))
                .andExpect(jsonPath("$.content[0].maxAtmosphericSpeed").exists());
    }

    @ParameterizedTest(name = "films?{0} -> {1} results")
    @CsvSource({"id=3&name=jedi, 1", "id=1&name=jedi, 0"})
    void filtersFilmsByIdAndName(String query, int expectedResults) throws Exception {
        swapi.stubFor(WireMock.get("/api/films").willReturn(json("films.json")));

        perform("/api/v1/films?" + query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(expectedResults));
    }

    @Test
    void listsFilmsFilteredByNameWithTitleExposedAsName() throws Exception {
        swapi.stubFor(WireMock.get("/api/films").willReturn(json("films.json")));

        perform("/api/v1/films?name=hope")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("A New Hope"))
                .andExpect(jsonPath("$.content[0].episodeId").value(4))
                .andExpect(jsonPath("$.content[0].title").doesNotExist());
    }

    @Test
    void idAndNameThatDoNotMatchReturnEmptyPage() throws Exception {
        swapi.stubFor(WireMock.get("/api/people/1").willReturn(json("people-1.json")));

        perform("/api/v1/people?id=1&name=vader")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void unknownIdReturnsEmptyPage() throws Exception {
        swapi.stubFor(WireMock.get("/api/people/9999").willReturn(json(404, "not-found.json")));

        perform("/api/v1/people?id=9999")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @ParameterizedTest(name = "{0} -> errors.{1}")
    @CsvSource({
            "page=0,                page",
            "size=51,               size",
            "page=abc,              page",
            "id=0,                  id",
            "id=abc,                id",
            "id=../films,           id",
            "id=1.5,                id",
            "name=<script>,         name"
    })
    void rejectsInvalidParametersWithoutCallingSwapi(String query, String field) throws Exception {
        perform("/api/v1/people?" + query)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors." + field).exists())
                .andExpect(content().string(not(containsString("script"))));

        swapi.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void rejectsNameLongerThan100Characters() throws Exception {
        perform("/api/v1/people?name=" + "a".repeat(101))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("must have at most 100 characters"));
    }

    @Test
    void ignoresBlankName() throws Exception {
        stubPeoplePage();

        mockMvc.perform(get("/api/v1/people")
                        .param("size", "2")
                        .param("name", "   ")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void swapiTimeoutReturnsGatewayTimeout() throws Exception {
        swapi.stubFor(WireMock.get(urlPathEqualTo("/api/people")).willReturn(okJson("{}").withFixedDelay(1_500)));

        perform("/api/v1/people")
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.detail").value("The Star Wars API did not respond in time"));
    }

    @Test
    void swapiRateLimitReturnsServiceUnavailableWithRetryAfter() throws Exception {
        swapi.stubFor(WireMock.get(urlPathEqualTo("/api/people"))
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "60")));

        perform("/api/v1/people")
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void swapiServerErrorReturnsBadGatewayWithoutInternalDetails() throws Exception {
        swapi.stubFor(WireMock.get(urlPathEqualTo("/api/people")).willReturn(serverError().withBody("stack trace")));

        perform("/api/v1/people")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("The Star Wars API is unavailable"))
                .andExpect(content().string(not(containsString("stack trace"))))
                .andExpect(content().string(not(containsString(swapi.baseUrl()))));
    }

    @Test
    void invalidSwapiResponseReturnsBadGateway() throws Exception {
        swapi.stubFor(WireMock.get(urlPathEqualTo("/api/people")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody("<html>maintenance</html>")));

        perform("/api/v1/people")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("The Star Wars API returned an invalid response"))
                .andExpect(content().string(not(containsString("maintenance"))));
    }

    @Test
    void repeatedRequestIsServedFromCache() throws Exception {
        stubPeoplePage();

        perform("/api/v1/people?size=2").andExpect(status().isOk());
        perform("/api/v1/people?size=2").andExpect(status().isOk());

        swapi.verify(1, anyRequestedFor(anyUrl()));
    }

    @Test
    void failuresAreNotCached() throws Exception {
        swapi.stubFor(WireMock.get(urlPathEqualTo("/api/people")).willReturn(serverError()));
        perform("/api/v1/people?size=2").andExpect(status().isBadGateway());

        stubPeoplePage();
        perform("/api/v1/people?size=2").andExpect(status().isOk());

        swapi.verify(RETRY_MAX_ATTEMPTS + 1, anyRequestedFor(anyUrl()));
    }

    @Test
    void requestWithoutTokenReturnsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/people"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401));

        swapi.verify(0, anyRequestedFor(anyUrl()));
    }

    private void stubPeoplePage() {
        stubPage("people", "people-page.json");
    }

    private void stubPage(String resource, String fixture) {
        swapi.stubFor(WireMock.get(urlPathEqualTo("/api/" + resource))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("limit", equalTo("2"))
                .withQueryParam("expanded", equalTo("true"))
                .willReturn(json(fixture)));
    }

    private ResultActions perform(String uri) throws Exception {
        return mockMvc.perform(get(uri).header("Authorization", "Bearer " + token));
    }
}
