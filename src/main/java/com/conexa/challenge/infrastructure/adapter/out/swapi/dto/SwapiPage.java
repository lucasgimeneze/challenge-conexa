package com.conexa.challenge.infrastructure.adapter.out.swapi.dto;

import java.util.List;
import java.util.Objects;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(SnakeCaseStrategy.class)
public record SwapiPage<P extends SwapiResourceProperties<?>>(
        Long totalRecords,
        Integer totalPages,
        List<SwapiItem<P>> results) {

    public SwapiPage {
        Objects.requireNonNull(totalRecords, "total_records");
        Objects.requireNonNull(totalPages, "total_pages");
        Objects.requireNonNull(results, "results");
    }
}
