package com.conexa.challenge.infrastructure.adapter.in.web;

import com.conexa.challenge.application.service.StarWarsQueryService;
import com.conexa.challenge.domain.model.Person;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "People", description = "Characters of the Star Wars universe")
@RestController
@RequestMapping("/api/v1/people")
public class PeopleController {

    private final StarWarsQueryService<Person> service;

    public PeopleController(StarWarsQueryService<Person> service) {
        this.service = service;
    }

    @Operation(operationId = "listPeople", summary = "List people", description = "Paginated list of people, filterable by id and/or name.")
    @StarWarsListResponses
    @GetMapping
    public PageResponse<Person> list(@Valid @ParameterObject ResourceQuery query) {
        return PageResponse.from(service.find(query.id(), query.name(), query.page(), query.size()));
    }
}
