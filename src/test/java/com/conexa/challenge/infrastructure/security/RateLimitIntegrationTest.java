package com.conexa.challenge.infrastructure.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conexa.challenge.application.service.StarWarsQueryService;
import com.conexa.challenge.domain.model.PageResult;
import com.conexa.challenge.domain.model.Person;
import com.conexa.challenge.domain.model.User;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {"rate-limit.auth.limit=2", "rate-limit.api.limit=2"})
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private StarWarsQueryService<Person> peopleService;

    @BeforeEach
    void setUp() {
        given(peopleService.find(any(), any(), anyInt(), anyInt())).willReturn(PageResult.empty(1, 10, 0));
    }

    @Test
    void rejectsCatalogRequestsOverTheUserLimit() throws Exception {
        String token = tokenFor(newUser());

        listPeople(token).andExpect(status().isOk());
        listPeople(token).andExpect(status().isOk());

        listPeople(token)
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void eachUserHasItsOwnLimit() throws Exception {
        String first = tokenFor(newUser());
        String second = tokenFor(newUser());

        listPeople(first);
        listPeople(first);
        listPeople(first).andExpect(status().isTooManyRequests());

        listPeople(second).andExpect(status().isOk());
    }

    @Test
    void rejectsAuthRequestsOverTheIpLimit() throws Exception {
        String ip = "10.0.0.1";

        login(ip).andExpect(status().isUnauthorized());
        login(ip).andExpect(status().isUnauthorized());

        login(ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        login("10.0.0.2").andExpect(status().isUnauthorized());
    }

    @Test
    void requestsWithoutTokenAreRejectedAsUnauthorizedNotLimited() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/people")).andExpect(status().isUnauthorized());
        }
    }

    private ResultActions listPeople(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/people").header("Authorization", "Bearer " + token));
    }

    private ResultActions login(String ip) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"nobody\", \"password\": \"wrong-password\"}"));
    }

    private String tokenFor(User user) {
        return tokenProvider.issue(user).value();
    }

    private static User newUser() {
        return new User(UUID.randomUUID(), "user-" + UUID.randomUUID(), "hash");
    }
}
