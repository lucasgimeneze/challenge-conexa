package com.conexa.challenge.infrastructure.security;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerReturnsCreatedWithoutExposingPassword() throws Exception {
        register("han", "millennium123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value("han"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerReturnsConflictWhenUsernameExists() throws Exception {
        register("chewie", "wookiee12345").andExpect(status().isCreated());

        register("chewie", "wookiee12345")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void registerReturnsBadRequestForInvalidInput() throws Exception {
        register("ab", "short").andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsBearerTokenForDemoUser() throws Exception {
        login("demo-test", "demo-test-pass")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void loginReturnsUnauthorizedForWrongPassword() throws Exception {
        login("demo-test", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void protectedRouteRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/people")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteRejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/people").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteAcceptsValidToken() throws Exception {
        String body = login("demo-test", "demo-test-pass").andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        // Todavía no hay endpoints de negocio: alcanza con verificar que pasa el filtro de seguridad.
        mockMvc.perform(get("/people").header("Authorization", "Bearer " + token))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    private ResultActions register(String username, String password) throws Exception {
        return postJson("/auth/register", username, password);
    }

    private ResultActions login(String username, String password) throws Exception {
        return postJson("/auth/login", username, password);
    }

    private ResultActions postJson(String path, String username, String password) throws Exception {
        String json = """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password);
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
