package com.conexa.challenge.domain.model;

import java.util.Objects;

public record Person(
        int id,
        String name,
        String gender,
        String birthYear,
        String height,
        String mass,
        String hairColor,
        String skinColor,
        String eyeColor) implements StarWarsResource {

    public Person {
        Objects.requireNonNull(name, "name is required");
    }
}
