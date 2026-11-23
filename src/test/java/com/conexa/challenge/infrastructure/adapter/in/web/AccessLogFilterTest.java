package com.conexa.challenge.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class AccessLogFilterTest {

    private final AccessLogFilter filter = new AccessLogFilter();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void logsMethodPathStatusDurationAndClient(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/people");
        request.setQueryString("size=2");
        request.setRemoteAddr("181.2.3.4");
        request.addHeader("Authorization", "Bearer secret-token");

        filter.doFilter(request, response, (req, res) -> response.setStatus(401));

        assertThat(output).containsPattern("GET /api/v1/people\\?size=2 401 \\(\\d+ ms\\) from 181\\.2\\.3\\.4");
        assertThat(output).doesNotContain("secret-token");
    }

    @Test
    void logsAServerErrorWhenTheRequestFails(CapturedOutput output) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");

        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new ServletException("boom");
        })).isInstanceOf(ServletException.class);

        assertThat(output).contains("POST /auth/login 500");
    }

    @Test
    void skipsTheHealthCheck(CapturedOutput output) throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/actuator/health"), response, (req, res) -> {
        });

        assertThat(output).doesNotContain("/actuator/health");
    }
}
