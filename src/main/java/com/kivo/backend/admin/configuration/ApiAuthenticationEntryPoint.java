package com.kivo.backend.admin.configuration;

import com.kivo.backend.common.web.ProblemResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ProblemResponseWriter problemResponseWriter;

  public ApiAuthenticationEntryPoint(ProblemResponseWriter problemResponseWriter) {
    this.problemResponseWriter = problemResponseWriter;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authenticationException)
      throws IOException, ServletException {
    response.setHeader("WWW-Authenticate", "Basic realm=\"kivo-admin\"");
    problemResponseWriter.write(
        response,
        HttpStatus.UNAUTHORIZED,
        "Authentication required",
        "Valid staff credentials are required for this endpoint",
        "AUTHENTICATION_REQUIRED");
  }
}
