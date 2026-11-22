package com.conexa.challenge.infrastructure.adapter.in.web;

import com.conexa.challenge.application.service.StarWarsQueryService;
import com.conexa.challenge.domain.model.Film;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Films", description = "Star Wars films (episodes I to VI)")
@RestController
@RequestMapping("/api/v1/films")
public class FilmsController {

    private final StarWarsQueryService<Film> service;

    public FilmsController(StarWarsQueryService<Film> service) {
        this.service = service;
    }

    @Operation(operationId = "listFilms", summary = "List films",
            description = "Paginated list of films, filterable by id and/or name. `name` is the film title.")
    @StarWarsListResponses
    @GetMapping
    public PageResponse<Film> list(@Valid @ParameterObject ResourceQuery query) {
        return PageResponse.from(service.find(query.id(), query.name(), query.page(), query.size()));
    }
}
