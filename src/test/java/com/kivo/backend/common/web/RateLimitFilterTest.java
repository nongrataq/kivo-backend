package com.kivo.backend.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.kivo.backend.common.configuration.RateLimitProperties;
import com.kivo.backend.testsupport.MutableClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class RateLimitFilterTest {

  @Test
  void rejectsOnlyMatchingPolicyAfterCapacityAndReturnsRetryAfter() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2026-08-14T10:00:00Z"), ZoneOffset.UTC);
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    RateLimitFilter filter = filter(clock, meters);
    AtomicInteger calls = new AtomicInteger();

    MockHttpServletResponse first = perform(filter, "POST", "/api/v1/feedback", calls);
    MockHttpServletResponse second = perform(filter, "POST", "/api/v1/feedback", calls);
    MockHttpServletResponse rejected = perform(filter, "POST", "/api/v1/feedback", calls);
    MockHttpServletResponse unmatched = perform(filter, "GET", "/api/v1/feedback", calls);

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(second.getStatus()).isEqualTo(200);
    assertThat(rejected.getStatus()).isEqualTo(429);
    assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
    assertThat(rejected.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    assertThat(unmatched.getStatus()).isEqualTo(200);
    assertThat(calls).hasValue(3);
    assertThat(meters.counter("kivo.http.rate_limit.rejected", "policy", "feedback").count())
        .isEqualTo(1);
  }

  @Test
  void isolatesClientsAndResetsWindow() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2026-08-14T10:00:00Z"), ZoneOffset.UTC);
    RateLimitFilter filter = filter(clock, new SimpleMeterRegistry());
    AtomicInteger calls = new AtomicInteger();

    perform(filter, "POST", "/api/v1/feedback", "192.0.2.1", calls);
    perform(filter, "POST", "/api/v1/feedback", "192.0.2.1", calls);
    MockHttpServletResponse otherClient =
        perform(filter, "POST", "/api/v1/feedback", "192.0.2.2", calls);
    clock.advance(Duration.ofMinutes(1));
    MockHttpServletResponse reset = perform(filter, "POST", "/api/v1/feedback", "192.0.2.1", calls);

    assertThat(otherClient.getStatus()).isEqualTo(200);
    assertThat(reset.getStatus()).isEqualTo(200);
  }

  private static RateLimitFilter filter(MutableClock clock, SimpleMeterRegistry meters) {
    Map<String, RateLimitProperties.Policy> policies = new LinkedHashMap<>();
    policies.put(
        "feedback",
        new RateLimitProperties.Policy("POST", "/api/v1/feedback", 2, Duration.ofMinutes(1)));
    RateLimitProperties properties = new RateLimitProperties(true, 100, policies);
    return new RateLimitFilter(
        properties, new ProblemResponseWriter(JsonMapper.builder().build()), meters, clock);
  }

  private static MockHttpServletResponse perform(
      RateLimitFilter filter, String method, String path, AtomicInteger calls) throws Exception {
    return perform(filter, method, path, "192.0.2.1", calls);
  }

  private static MockHttpServletResponse perform(
      RateLimitFilter filter, String method, String path, String remoteAddress, AtomicInteger calls)
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRemoteAddr(remoteAddress);
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(
        request, response, (ignoredRequest, ignoredResponse) -> calls.incrementAndGet());
    return response;
  }
}
