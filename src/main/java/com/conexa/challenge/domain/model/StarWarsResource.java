package com.conexa.challenge.domain.model;

import java.util.Locale;

public sealed interface StarWarsResource permits Person, Film, Starship, Vehicle {

    int id();

    String name();

    default boolean nameContains(String text) {
        return name().toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT));
    }
}
