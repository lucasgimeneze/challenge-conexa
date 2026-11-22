package com.conexa.challenge.domain.model;

import java.util.Objects;

public record Starship(
        int id,
        String name,
        String model,
        String manufacturer,
        String starshipClass,
        String costInCredits,
        String length,
        String crew,
        String passengers,
        String maxAtmosphericSpeed,
        String hyperdriveRating,
        String cargoCapacity) implements StarWarsResource {

    public Starship {
        Objects.requireNonNull(name, "name is required");
    }
}
