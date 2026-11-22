package com.conexa.challenge.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

class RateLimitFilterTest {

    private static final Duration WINDOW = Duration.ofMillis(300);

    private final AtomicInteger rejected = new AtomicInteger();
    private final AtomicInteger passed = new AtomicInteger();
    private final HandlerExceptionResolver resolver = (request, response, handler, ex) -> {
        rejected.incrementAndGet();
        return new ModelAndView();
    };

    @Test
    void invalidPeriodFailsWhenTheFilterIsCreated() {
        RateLimitProperties properties = properties(new RateLimitProperties.Limit(2, Duration.ZERO));

        assertThatThrownBy(() -> new RateLimitFilter(properties, resolver))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void quotaIsRestoredWhenTheWindowEnds() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(properties(new RateLimitProperties.Limit(2, WINDOW)), resolver);

        loginFrom(filter);
        loginFrom(filter);
        loginFrom(filter);
        assertThat(passed).hasValue(2);
        assertThat(rejected).hasValue(1);

        Thread.sleep(WINDOW.plusMillis(100));
        loginFrom(filter);

        assertThat(passed).hasValue(3);
    }

    private void loginFrom(RateLimitFilter filter) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("10.0.0.9");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> passed.incrementAndGet());
    }

    private static RateLimitProperties properties(RateLimitProperties.Limit auth) {
        return new RateLimitProperties(auth, new RateLimitProperties.Limit(100, Duration.ofMinutes(15)));
    }
}
