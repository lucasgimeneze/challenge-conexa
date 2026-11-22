package com.conexa.challenge.infrastructure.adapter.out.swapi.dto;

import java.util.Objects;

public record SwapiItem<P extends SwapiResourceProperties<?>>(Integer uid, P properties) {

    public SwapiItem {
        Objects.requireNonNull(uid, "uid");
        Objects.requireNonNull(properties, "properties");
    }
}
