package com.conexa.challenge.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final AtomicReference<String> idSeenDuringRequest = new AtomicReference<>();

    @Test
    void generatesAnIdWhenTheRequestHasNone() throws Exception {
        runFilter();

        String id = response.getHeader(CorrelationIdFilter.HEADER);
        assertThat(UUID.fromString(id)).isNotNull();
        assertThat(idSeenDuringRequest.get()).isEqualTo(id);
    }

    @Test
    void keepsAValidIdFromTheRequest() throws Exception {
        request.addHeader(CorrelationIdFilter.HEADER, "checkout-42");

        runFilter();

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("checkout-42");
        assertThat(idSeenDuringRequest.get()).isEqualTo("checkout-42");
    }

    @ParameterizedTest
    @ValueSource(strings = {"bad id", "id\nFAKE LOG LINE", "<script>"})
    void replacesAnInvalidIdToKeepLogsClean(String invalidId) throws Exception {
        request.addHeader(CorrelationIdFilter.HEADER, invalidId);

        runFilter();

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isNotEqualTo(invalidId);
    }

    @Test
    void clearsTheMdcAfterTheRequest() throws Exception {
        runFilter();

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    private void runFilter() throws Exception {
        filter.doFilter(request, response,
                (req, res) -> idSeenDuringRequest.set(MDC.get(CorrelationIdFilter.MDC_KEY)));
    }
}
