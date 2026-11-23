package com.conexa.challenge.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    private static final String DESCRIPTION = """
            Lists People, Films, Starships and Vehicles from the [Star Wars API](https://www.swapi.tech), \
            paginated and filterable by id and/or name.

            **How to authenticate**
            1. `POST /auth/register` with a username and password.
            2. `POST /auth/login` with the same credentials to get an `accessToken`.
            3. Click **Authorize** and paste the token.
            """;

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Conexa Star Wars API")
                        .version("v1")
                        .description(DESCRIPTION))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    /**
     * El schema que springdoc genera para {@code ProblemDetail} anida los campos extra en "properties" y suma
     * {@code type} e {@code instance}, pero la API los devuelve en la raíz y nunca envía los otros dos.
     */
    @Bean
    OpenApiCustomizer problemDetailSchema() {
        return openApi -> openApi.getComponents().addSchemas("ProblemDetail", problemDetail());
    }

    private static Schema<?> problemDetail() {
        ObjectSchema problem = new ObjectSchema();
        problem.setDescription("Error in RFC 9457 format");
        problem.addProperty("title", new StringSchema().example("Bad Request"));
        problem.addProperty("status", new IntegerSchema().example(400));
        problem.addProperty("detail", new StringSchema().example("Invalid request content."));
        problem.addProperty("errors", fieldErrors());
        problem.addProperty("correlationId", new StringSchema()
                .description("Request id to report issues; also sent in the `X-Correlation-Id` header.")
                .example("3f1c2b9e-7a4d-4e1b-9c55-2d8f6a0b1e7c"));
        problem.setRequired(List.of("title", "status", "detail", "correlationId"));
        return problem;
    }

    private static Schema<?> fieldErrors() {
        return new MapSchema()
                .additionalProperties(new StringSchema())
                .description("Only in validation errors: invalid field mapped to its message.")
                .example(Map.of("page", "must be between 1 and 10000"));
    }
}
