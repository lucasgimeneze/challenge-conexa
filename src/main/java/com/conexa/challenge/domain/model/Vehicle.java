package com.conexa.challenge.domain.model;

import java.util.Objects;

public record Vehicle(
        int id,
        String name,
        String model,
        String manufacturer,
        String vehicleClass,
        String costInCredits,
        String length,
        String crew,
        String passengers,
        String maxAtmosphericSpeed,
        String cargoCapacity) implements StarWarsResource {

    public Vehicle {
        Objects.requireNonNull(name, "name is required");
    }
}
