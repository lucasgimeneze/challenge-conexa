package com.conexa.challenge.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conexa.challenge.domain.model.User;
import com.conexa.challenge.infrastructure.security.JwtTokenProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void errorResponsesCarryTheCorrelationIdInHeaderAndBody() throws Exception {
        mockMvc.perform(get("/api/v1/people").header(CorrelationIdFilter.HEADER, "trace-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(CorrelationIdFilter.HEADER, "trace-123"))
                .andExpect(jsonPath("$.correlationId").value("trace-123"));
    }

    @Test
    void accessLogRecordsRejectedRequestsWithTheirCorrelationId(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/v1/people").header(CorrelationIdFilter.HEADER, "trace-log"))
                .andExpect(status().isUnauthorized());

        assertThat(output).containsPattern("\\[trace-log\\].*GET /api/v1/people 401");
    }

    @Test
    void healthIsPublicAndHidesDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(header().string(CorrelationIdFilter.HEADER, not("")));
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        String token = tokenProvider.issue(new User(UUID.randomUUID(), "luke", "hash")).value();

        mockMvc.perform(get("/actuator/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/metrics").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
