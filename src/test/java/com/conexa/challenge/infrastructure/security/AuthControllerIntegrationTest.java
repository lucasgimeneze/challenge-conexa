package com.conexa.challenge.infrastructure.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conexa.challenge.domain.model.User;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    // Ruta protegida cualquiera: todavía no hay endpoints de negocio.
    private static final String PROTECTED_PATH = "/people";
    private static final String PASSWORD = "skywalker123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private AuthProperties authProperties;

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
    void registerReportsInvalidFieldsWithoutEchoingValues() throws Exception {
        register("ab", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username")
                        .value("must have 3 to 50 characters: letters, digits, '.', '_' or '-'"))
                .andExpect(jsonPath("$.errors.password").value("must have between 8 and 72 characters"))
                .andExpect(jsonPath("$..rejectedValue").isEmpty());
    }

    @Test
    void registerRejectsUsernameWithSpaces() throws Exception {
        register("luke ", "skywalker123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void registerRejectsPasswordLongerThan72Characters() throws Exception {
        register("leia", "a".repeat(73))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("must have between 8 and 72 characters"));
    }

    @Test
    void registerRejectsNonAsciiPassword() throws Exception {
        register("leia", "contraseña123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password")
                        .value("may only contain letters, digits, spaces and symbols (no accents or emojis)"));
    }

    @Test
    void registerAcceptsPasswordOfExactly72AsciiCharacters() throws Exception {
        register("leia", "a".repeat(72)).andExpect(status().isCreated());
    }

    @Test
    void loginReturnsBearerTokenForRegisteredUser() throws Exception {
        String username = registerNewUser();

        login(username, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void loginReturnsUnauthorizedForWrongPassword() throws Exception {
        String username = registerNewUser();

        login(username, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void protectedRouteRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteRejectsInvalidToken() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteRejectsExpiredToken() throws Exception {
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        JwtTokenProvider pastProvider = new JwtTokenProvider(jwtEncoder, authProperties, twoHoursAgo);
        String expiredToken = pastProvider.issue(new User(UUID.randomUUID(), "luke", "hash")).value();

        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteAcceptsValidToken() throws Exception {
        String body = login(registerNewUser(), PASSWORD).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        // 404 (y no 401) prueba que el request pasó el filtro de seguridad y llegó al dispatcher.
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // El repositorio en memoria se comparte entre tests: cada uno usa un username propio.
    private String registerNewUser() throws Exception {
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
        register(username, PASSWORD).andExpect(status().isCreated());
        return username;
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
