package com.kivo.backend.common.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kivo.backend.common.configuration.RateLimitProperties;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

public class RateLimitFilter extends OncePerRequestFilter {

  private final boolean enabled;
  private final List<CompiledPolicy> policies;
  private final Cache<ClientPolicyKey, WindowCounter> counters;
  private final ProblemResponseWriter problemResponseWriter;
  private final MeterRegistry meterRegistry;
  private final Clock clock;

  public RateLimitFilter(
      RateLimitProperties properties,
      ProblemResponseWriter problemResponseWriter,
      MeterRegistry meterRegistry,
      Clock clock) {
    this.enabled = properties.enabled();
    this.policies = compile(properties);
    this.counters =
        Caffeine.newBuilder()
            .maximumSize(properties.maximumClients())
            .expireAfterAccess(longestWindow(properties).multipliedBy(2))
            .build();
    this.problemResponseWriter = problemResponseWriter;
    this.meterRegistry = meterRegistry;
    this.clock = clock;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    CompiledPolicy policy = findPolicy(request);
    if (!enabled || policy == null) {
      filterChain.doFilter(request, response);
      return;
    }

    Instant now = clock.instant();
    ClientPolicyKey key = new ClientPolicyKey(policy.id(), request.getRemoteAddr());
    WindowCounter counter =
        counters.get(key, ignored -> new WindowCounter(now, policy.policy().window()));
    Decision decision = counter.acquire(now, policy.policy());
    if (decision.allowed()) {
      filterChain.doFilter(request, response);
      return;
    }

    response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
    meterRegistry.counter("kivo.http.rate_limit.rejected", "policy", policy.id()).increment();
    problemResponseWriter.write(
        response,
        HttpStatus.TOO_MANY_REQUESTS,
        "Rate limit exceeded",
        "Too many requests; retry after %d second(s)".formatted(decision.retryAfterSeconds()),
        "RATE_LIMIT_EXCEEDED");
  }

  private CompiledPolicy findPolicy(HttpServletRequest request) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return null;
    }
    PathContainer path = PathContainer.parsePath(request.getRequestURI());
    return policies.stream()
        .filter(policy -> policy.method().equalsIgnoreCase(request.getMethod()))
        .filter(policy -> policy.pathPattern().matches(path))
        .findFirst()
        .orElse(null);
  }

  private static List<CompiledPolicy> compile(RateLimitProperties properties) {
    PathPatternParser parser = new PathPatternParser();
    return properties.policies().entrySet().stream()
        .map(
            entry ->
                new CompiledPolicy(
                    entry.getKey(),
                    entry.getValue().method().toUpperCase(Locale.ROOT),
                    parser.parse(entry.getValue().path()),
                    entry.getValue()))
        .toList();
  }

  private static Duration longestWindow(RateLimitProperties properties) {
    return properties.policies().values().stream()
        .map(RateLimitProperties.Policy::window)
        .max(Duration::compareTo)
        .orElse(Duration.ofMinutes(1));
  }

  private record CompiledPolicy(
      String id, String method, PathPattern pathPattern, RateLimitProperties.Policy policy) {}

  private record ClientPolicyKey(String policyId, String clientAddress) {}

  private record Decision(boolean allowed, long retryAfterSeconds) {

    static Decision permit() {
      return new Decision(true, 0);
    }

    static Decision reject(long retryAfterSeconds) {
      return new Decision(false, retryAfterSeconds);
    }
  }

  private static final class WindowCounter {

    private Instant windowStartedAt;
    private Instant resetsAt;
    private int count;

    private WindowCounter(Instant now, Duration window) {
      reset(now, window);
    }

    synchronized Decision acquire(Instant now, RateLimitProperties.Policy policy) {
      if (!now.isBefore(resetsAt)) {
        reset(now, policy.window());
      }
      if (count >= policy.capacity()) {
        long millis = Math.max(1, Duration.between(now, resetsAt).toMillis());
        return Decision.reject(Math.max(1, (millis + 999) / 1000));
      }
      count++;
      return Decision.permit();
    }

    private void reset(Instant now, Duration window) {
      windowStartedAt = now;
      resetsAt = now.plus(window);
      count = 0;
    }
  }
}
