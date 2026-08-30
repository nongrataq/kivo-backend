package com.kivo.backend.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void preservesValidCorrelationIdDuringRequestAndReturnsIt() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/routes");
    request.addHeader(CorrelationIdFilter.HEADER_NAME, "kivo-request-1234");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> observed = new AtomicReference<>();
    FilterChain chain = (ignoredRequest, ignoredResponse) -> observed.set(MDC.get("correlationId"));

    filter.doFilter(request, response, chain);

    assertThat(observed).hasValue("kivo-request-1234");
    assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("kivo-request-1234");
    assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
  }

  @Test
  void replacesInvalidCorrelationIdToPreventLogInjection() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/routes");
    request.addHeader(CorrelationIdFilter.HEADER_NAME, "bad\nvalue");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

    assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
        .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
  }
}
