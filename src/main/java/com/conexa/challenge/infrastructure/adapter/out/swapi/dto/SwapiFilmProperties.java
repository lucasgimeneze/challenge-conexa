package com.conexa.challenge.infrastructure.adapter.out.swapi.dto;

import com.conexa.challenge.domain.model.Film;
import java.util.Objects;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(SnakeCaseStrategy.class)
public record SwapiFilmProperties(
        String title,
        Integer episodeId,
        String director,
        String producer,
        String releaseDate,
        String openingCrawl) implements SwapiResourceProperties<Film> {

    public SwapiFilmProperties {
        Objects.requireNonNull(title, "title");
    }

    @Override
    public Film toDomain(int id) {
        return new Film(id, title, episodeId, director, producer, releaseDate, openingCrawl);
    }
}
