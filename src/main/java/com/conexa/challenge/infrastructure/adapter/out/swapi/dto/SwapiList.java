package com.conexa.challenge.infrastructure.adapter.out.swapi.dto;

import java.util.List;
import java.util.Objects;

public record SwapiList<P extends SwapiResourceProperties<?>>(List<SwapiItem<P>> result) {

    public SwapiList {
        Objects.requireNonNull(result, "result");
    }
}
