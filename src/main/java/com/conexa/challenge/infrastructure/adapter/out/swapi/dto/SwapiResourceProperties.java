package com.conexa.challenge.infrastructure.adapter.out.swapi.dto;

import com.conexa.challenge.domain.model.StarWarsResource;

public interface SwapiResourceProperties<T extends StarWarsResource> {

    T toDomain(int id);
}
