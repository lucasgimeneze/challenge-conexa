package com.conexa.challenge.infrastructure.adapter.out.swapi;

import static com.conexa.challenge.infrastructure.adapter.out.swapi.SwapiFixtures.json;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.conexa.challenge.domain.model.Film;
import com.conexa.challenge.domain.model.PageResult;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class FilmsSwapiCatalogTest {

    @RegisterExtension
    static final WireMockExtension swapi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private FilmsSwapiCatalog catalog;

    @BeforeEach
    void setUp() {
        swapi.stubFor(get("/films").willReturn(json("films.json")));
        catalog = new FilmsSwapiCatalog(SwapiFixtures.client(swapi.baseUrl()));
    }

    @Test
    void paginatesAllFilmsInMemory() {
        PageResult<Film> page = catalog.findPage(2, 4);

        assertThat(page.content()).extracting(Film::name).containsExactly("Attack of the Clones", "Revenge of the Sith");
        assertThat(page.totalElements()).isEqualTo(6);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void exposesTitleAsName() {
        Film film = catalog.findById(1).orElseThrow();

        assertThat(film.name()).isEqualTo("A New Hope");
        assertThat(film.episodeId()).isEqualTo(4);
        assertThat(film.releaseDate()).isEqualTo("1977-05-25");
    }

    @Test
    void findsByPartialNameIgnoringCase() {
        assertThat(catalog.findByName("JEDI")).extracting(Film::name).containsExactly("Return of the Jedi");
    }

    @Test
    void unknownIdIsEmpty() {
        assertThat(catalog.findById(7)).isEmpty();
    }

    @Test
    void neverCallsTheSearchOrDetailEndpoints() {
        catalog.findById(1);
        catalog.findByName("hope");

        swapi.verify(2, anyRequestedFor(anyUrl()));
        swapi.verify(2, getRequestedFor(urlEqualTo("/films")));
    }
}
