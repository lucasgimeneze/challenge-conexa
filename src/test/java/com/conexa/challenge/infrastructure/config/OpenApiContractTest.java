package com.conexa.challenge.infrastructure.config;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsTheApiAndTheBearerScheme() throws Exception {
        apiDocs()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Conexa Star Wars API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray());
    }

    @ParameterizedTest
    @CsvSource({"people, listPeople", "films, listFilms", "starships, listStarships", "vehicles, listVehicles"})
    void documentsEachListEndpoint(String resource, String operationId) throws Exception {
        String operation = "$.paths['/api/v1/" + resource + "'].get";

        apiDocs()
                .andExpect(jsonPath(operation + ".operationId").value(operationId))
                .andExpect(jsonPath(operation + ".parameters[*].name").value(containsInAnyOrder("page", "size", "id", "name")))
                .andExpect(jsonPath(operation + ".parameters[?(@.name == 'page')].schema.type").value(hasItems("integer")))
                .andExpect(jsonPath(operation + ".parameters[?(@.name == 'page')].schema.default").value(hasItems(1)))
                .andExpect(jsonPath(operation + ".parameters[?(@.name == 'size')].schema.default").value(hasItems(10)))
                .andExpect(jsonPath(operation + ".parameters[?(@.name == 'size')].schema.maximum").value(hasItems(50)))
                .andExpect(jsonPath(operation + ".responses['200'].content['*/*'].schema['$ref']").exists())
                .andExpect(jsonPath(operation + ".responses['400'].content['application/problem+json']").exists())
                .andExpect(jsonPath(operation + ".responses['401']").exists())
                .andExpect(jsonPath(operation + ".responses['429']").exists())
                .andExpect(jsonPath(operation + ".responses['502']").exists())
                .andExpect(jsonPath(operation + ".responses['503']").exists())
                .andExpect(jsonPath(operation + ".responses['504']").exists());
    }

    @Test
    void documentsCamelCaseResourceSchemas() throws Exception {
        apiDocs()
                .andExpect(jsonPath("$.components.schemas.Person.properties.birthYear").exists())
                .andExpect(jsonPath("$.components.schemas.Film.properties.name").exists())
                .andExpect(jsonPath("$.components.schemas.Film.properties.title").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.Starship.properties.maxAtmosphericSpeed").exists())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail").exists());
    }

    @Test
    void authEndpointsDoNotRequireAToken() throws Exception {
        apiDocs()
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/auth/register'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/auth/register'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['429']").exists());
    }

    private ResultActions apiDocs() throws Exception {
        return mockMvc.perform(get("/v3/api-docs"));
    }
}
