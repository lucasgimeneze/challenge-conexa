package com.conexa.challenge.infrastructure.adapter.out.swapi;

import com.conexa.challenge.domain.model.Film;
import com.conexa.challenge.domain.model.PageResult;
import com.conexa.challenge.domain.port.out.StarWarsCatalog;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiFilmProperties;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiItem;
import com.conexa.challenge.infrastructure.adapter.out.swapi.dto.SwapiList;
import java.util.List;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;

/**
 * SWAPI no pagina /films y siempre devuelve las 6 películas completas: con una sola llamada (cacheada)
 * se resuelven en memoria el listado y los filtros.
 */
public class FilmsSwapiCatalog implements StarWarsCatalog<Film> {

    private static final ParameterizedTypeReference<SwapiList<SwapiFilmProperties>> FILMS_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final SwapiClient client;

    public FilmsSwapiCatalog(SwapiClient client) {
        this.client = client;
    }

    @Override
    public PageResult<Film> findPage(int page, int size) {
        return PageResult.of(allFilms(), page, size);
    }

    @Override
    public Optional<Film> findById(int id) {
        return allFilms().stream()
                .filter(film -> film.id() == id)
                .findFirst();
    }

    @Override
    public List<Film> findByName(String name) {
        return allFilms().stream()
                .filter(film -> film.nameContains(name))
                .toList();
    }

    private List<Film> allFilms() {
        return client.fetch(FILMS_TYPE, "/films").result().stream()
                .map(FilmsSwapiCatalog::toDomain)
                .toList();
    }

    private static Film toDomain(SwapiItem<SwapiFilmProperties> item) {
        return item.properties().toDomain(item.uid());
    }
}
