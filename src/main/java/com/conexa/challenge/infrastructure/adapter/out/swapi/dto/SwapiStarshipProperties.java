package com.conexa.challenge.infrastructure.adapter.out.swapi.dto;

import com.conexa.challenge.domain.model.Starship;
import java.util.Objects;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(SnakeCaseStrategy.class)
public record SwapiStarshipProperties(
        String name,
        String model,
        String manufacturer,
        String starshipClass,
        String costInCredits,
        String length,
        String crew,
        String passengers,
        String maxAtmospheringSpeed,
        String hyperdriveRating,
        String cargoCapacity) implements SwapiResourceProperties<Starship> {

    public SwapiStarshipProperties {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public Starship toDomain(int id) {
        return new Starship(id, name, model, manufacturer, starshipClass, costInCredits, length, crew, passengers,
                maxAtmospheringSpeed, hyperdriveRating, cargoCapacity);
    }
}
