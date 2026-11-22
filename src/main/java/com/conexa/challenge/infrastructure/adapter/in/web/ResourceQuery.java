package com.conexa.challenge.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ResourceQuery(
        @Parameter(description = "Page number, starting at 1.",
                schema = @Schema(type = "integer", format = "int32", defaultValue = "1"))
        @Min(value = 1, message = "must be between 1 and 10000")
        @Max(value = 10_000, message = "must be between 1 and 10000")
        Integer page,

        @Parameter(description = "Page size.",
                schema = @Schema(type = "integer", format = "int32", defaultValue = "10"))
        @Min(value = 1, message = "must be between 1 and 50")
        @Max(value = 50, message = "must be between 1 and 50")
        Integer size,

        @Parameter(description = "Filters by id. Combined with `name`, both must match.", example = "1")
        @Positive(message = "must be a positive number")
        Integer id,

        @Parameter(description = "Filters by name: partial match, case insensitive. Blank is ignored.", example = "sky")
        @Size(max = 100, message = "must have at most 100 characters")
        @Pattern(regexp = "[\\p{L}\\p{N} .'/()-]*",
                message = "may only contain letters, digits, spaces and . ' / ( ) -")
        String name) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;

    public ResourceQuery {
        page = page == null ? DEFAULT_PAGE : page;
        size = size == null ? DEFAULT_SIZE : size;
        name = name == null || name.isBlank() ? null : name.strip();
    }
}
