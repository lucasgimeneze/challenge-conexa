package com.conexa.challenge.infrastructure.adapter.in.web;

import com.conexa.challenge.application.service.StarWarsQueryService;
import com.conexa.challenge.domain.model.Starship;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Starships", description = "Transport crafts with hyperdrive capability")
@RestController
@RequestMapping("/api/v1/starships")
public class StarshipsController {

    private final StarWarsQueryService<Starship> service;

    public StarshipsController(StarWarsQueryService<Starship> service) {
        this.service = service;
    }

    @Operation(operationId = "listStarships", summary = "List starships", description = "Paginated list of starships, filterable by id and/or name.")
    @StarWarsListResponses
    @GetMapping
    public PageResponse<Starship> list(@Valid @ParameterObject ResourceQuery query) {
        return PageResponse.from(service.find(query.id(), query.name(), query.page(), query.size()));
    }
}
