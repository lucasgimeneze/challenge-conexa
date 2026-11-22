package com.conexa.challenge.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
}
