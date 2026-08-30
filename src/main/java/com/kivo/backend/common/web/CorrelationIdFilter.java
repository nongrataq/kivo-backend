package com.kivo.backend.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-Correlation-ID";
  public static final String MDC_KEY = "correlationId";

  private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
  private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{7,127}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));
    long startedAt = System.nanoTime();
    MDC.put(MDC_KEY, correlationId);
    response.setHeader(HEADER_NAME, correlationId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
      log.atInfo()
          .addKeyValue("httpMethod", request.getMethod())
          .addKeyValue("path", request.getRequestURI())
          .addKeyValue("status", response.getStatus())
          .addKeyValue("durationMs", durationMillis)
          .addKeyValue("remoteAddress", request.getRemoteAddr())
          .log("HTTP request completed");
      MDC.remove(MDC_KEY);
    }
  }

  static String resolveCorrelationId(String supplied) {
    return supplied != null && VALID_ID.matcher(supplied).matches()
        ? supplied
        : UUID.randomUUID().toString();
  }
}
