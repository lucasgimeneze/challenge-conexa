package com.conexa.challenge.domain.model;

import java.util.Objects;

public record Film(
        int id,
        String name,
        Integer episodeId,
        String director,
        String producer,
        String releaseDate,
        String openingCrawl) implements StarWarsResource {

    public Film {
        Objects.requireNonNull(name, "name is required");
    }
}
