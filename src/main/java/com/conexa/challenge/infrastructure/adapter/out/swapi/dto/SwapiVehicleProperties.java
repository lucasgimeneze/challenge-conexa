package com.conexa.challenge.infrastructure.adapter.out.swapi.dto;

import com.conexa.challenge.domain.model.Vehicle;
import java.util.Objects;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(SnakeCaseStrategy.class)
public record SwapiVehicleProperties(
        String name,
        String model,
        String manufacturer,
        String vehicleClass,
        String costInCredits,
        String length,
        String crew,
        String passengers,
        String maxAtmospheringSpeed,
        String cargoCapacity) implements SwapiResourceProperties<Vehicle> {

    public SwapiVehicleProperties {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public Vehicle toDomain(int id) {
        return new Vehicle(id, name, model, manufacturer, vehicleClass, costInCredits, length, crew, passengers,
                maxAtmospheringSpeed, cargoCapacity);
    }
}
