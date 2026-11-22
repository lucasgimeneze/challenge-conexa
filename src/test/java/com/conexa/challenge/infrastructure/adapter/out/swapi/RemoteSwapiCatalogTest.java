package com.conexa.challenge.infrastructure.adapter.out.swapi;

import static com.conexa.challenge.infrastructure.adapter.out.swapi.SwapiFixtures.json;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.conexa.challenge.domain.model.PageResult;
import com.conexa.challenge.domain.model.Person;
import com.conexa.challenge.domain.model.Starship;
import com.conexa.challenge.domain.model.Vehicle;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiPersonProperties;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiStarshipProperties;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiVehicleProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class RemoteSwapiCatalogTest {

    @RegisterExtension
    static final WireMockExtension swapi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void mapsExpandedPageToDomain() {
        stubPage("people", 1, "people-page.json");

        PageResult<Person> page = catalog().findPage(1, 2);

        assertThat(page.totalElements()).isEqualTo(82);
        assertThat(page.totalPages()).isEqualTo(41);
        assertThat(page.content()).hasSize(2);
        assertThat(page.content().getFirst())
                .isEqualTo(new Person(1, "Luke Skywalker", "male", "19BBY", "172", "77", "blond", "fair", "blue"));
    }

    @Test
    void pageOutOfRangeIsEmptyEvenThoughSwapiReturnsTheLastPage() {
        stubPage("people", 99, "people-page-out-of-range.json");

        PageResult<Person> page = catalog().findPage(99, 2);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(82);
    }

    @Test
    void findsByName() {
        swapi.stubFor(get(urlPathEqualTo("/people")).withQueryParam("name", equalTo("sky"))
                .willReturn(json("people-search-sky.json")));

        assertThat(catalog().findByName("sky"))
                .extracting(Person::id, Person::name)
                .containsExactly(
                        tuple(1, "Luke Skywalker"),
                        tuple(11, "Anakin Skywalker"),
                        tuple(43, "Shmi Skywalker"));
    }

    @Test
    void escapesRegexCharactersSoTheSearchIsLiteral() {
        swapi.stubFor(get(urlPathEqualTo("/people")).withQueryParam("name", equalTo("sky\\(\\.\\)"))
                .willReturn(json("empty-search.json")));

        assertThat(catalog().findByName("sky(.)")).isEmpty();
    }

    @Test
    void searchWithoutMatchesIsEmpty() {
        swapi.stubFor(get(urlPathEqualTo("/people")).withQueryParam("name", equalTo("zzzz"))
                .willReturn(json("empty-search.json")));

        assertThat(catalog().findByName("zzzz")).isEmpty();
    }

    @Test
    void findsById() {
        swapi.stubFor(get("/people/1").willReturn(json("people-1.json")));

        assertThat(catalog().findById(1)).map(Person::name).contains("Luke Skywalker");
    }

    @Test
    void unknownIdIsEmptyWhenSwapiAnswers404() {
        swapi.stubFor(get("/people/9999").willReturn(json(404, "not-found.json")));

        assertThat(catalog().findById(9999)).isEmpty();
    }

    @Test
    void unknownIdIsEmptyWhenSwapiAnswers200WithoutResult() {
        swapi.stubFor(get("/people/9999").willReturn(json("not-found.json")));

        assertThat(catalog().findById(9999)).isEmpty();
    }

    @Test
    void mapsStarshipFieldsToCamelCase() {
        stubPage("starships", 1, "starships-page.json");

        Starship corvette = new RemoteSwapiCatalog<>(client(), "starships", SwapiStarshipProperties.class)
                .findPage(1, 2).content().getFirst();

        assertThat(corvette.id()).isEqualTo(2);
        assertThat(corvette.starshipClass()).isEqualTo("corvette");
        assertThat(corvette.maxAtmosphericSpeed()).isEqualTo("950");
        assertThat(corvette.hyperdriveRating()).isEqualTo("2.0");
    }

    @Test
    void mapsVehicleFieldsToCamelCase() {
        stubPage("vehicles", 1, "vehicles-page.json");

        Vehicle sandCrawler = new RemoteSwapiCatalog<>(client(), "vehicles", SwapiVehicleProperties.class)
                .findPage(1, 2).content().getFirst();

        assertThat(sandCrawler.id()).isEqualTo(4);
        assertThat(sandCrawler.vehicleClass()).isEqualTo("wheeled");
        assertThat(sandCrawler.costInCredits()).isEqualTo("150000");
    }

    @Test
    void keepsUnknownValuesAsText() {
        swapi.stubFor(get("/people/43").willReturn(okJson("""
                {"result": {"uid": "43", "properties": {"name": "Shmi Skywalker", "mass": "unknown"}}}
                """)));

        Person shmi = catalog().findById(43).orElseThrow();

        assertThat(shmi.mass()).isEqualTo("unknown");
        assertThat(shmi.height()).isNull();
    }

    private static void stubPage(String resource, int page, String fixture) {
        swapi.stubFor(get(urlPathEqualTo("/" + resource))
                .withQueryParam("page", equalTo(String.valueOf(page)))
                .withQueryParam("limit", equalTo("2"))
                .withQueryParam("expanded", equalTo("true"))
                .willReturn(json(fixture)));
    }

    private static RemoteSwapiCatalog<Person, SwapiPersonProperties> catalog() {
        return new RemoteSwapiCatalog<>(client(), "people", SwapiPersonProperties.class);
    }

    private static SwapiClient client() {
        SwapiProperties properties = new SwapiProperties(swapi.baseUrl(), Duration.ofSeconds(1), Duration.ofSeconds(1));
        return new SwapiClient(properties, new SwapiErrorTranslator());
    }
}
