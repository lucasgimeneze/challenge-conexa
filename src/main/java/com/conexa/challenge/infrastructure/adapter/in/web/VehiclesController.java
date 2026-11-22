package com.conexa.challenge.infrastructure.adapter.in.web;

import com.conexa.challenge.application.service.StarWarsQueryService;
import com.conexa.challenge.domain.model.Vehicle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Vehicles", description = "Transport crafts without hyperdrive capability")
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehiclesController {

    private final StarWarsQueryService<Vehicle> service;

    public VehiclesController(StarWarsQueryService<Vehicle> service) {
        this.service = service;
    }

    @Operation(operationId = "listVehicles", summary = "List vehicles", description = "Paginated list of vehicles, filterable by id and/or name.")
    @StarWarsListResponses
    @GetMapping
    public PageResponse<Vehicle> list(@Valid @ParameterObject ResourceQuery query) {
        return PageResponse.from(service.find(query.id(), query.name(), query.page(), query.size()));
    }
}
