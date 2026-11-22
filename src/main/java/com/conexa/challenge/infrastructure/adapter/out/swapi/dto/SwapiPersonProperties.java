package com.conexa.challenge.infrastructure.adapter.out.swapi.dto;

import com.conexa.challenge.domain.model.Person;
import java.util.Objects;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(SnakeCaseStrategy.class)
public record SwapiPersonProperties(
        String name,
        String gender,
        String birthYear,
        String height,
        String mass,
        String hairColor,
        String skinColor,
        String eyeColor) implements SwapiResourceProperties<Person> {

    public SwapiPersonProperties {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public Person toDomain(int id) {
        return new Person(id, name, gender, birthYear, height, mass, hairColor, skinColor, eyeColor);
    }
}
