package com.conexa.challenge.infrastructure.adapter.out.swapi.dto;

import java.util.Optional;

public record SwapiSingle<P extends SwapiResourceProperties<?>>(SwapiItem<P> result) {

    public Optional<SwapiItem<P>> item() {
        return Optional.ofNullable(result);
    }
}
