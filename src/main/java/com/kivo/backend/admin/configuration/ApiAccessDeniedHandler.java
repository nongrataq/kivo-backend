package com.kivo.backend.admin.configuration;

import com.kivo.backend.common.web.ProblemResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

  private final ProblemResponseWriter problemResponseWriter;

  public ApiAccessDeniedHandler(ProblemResponseWriter problemResponseWriter) {
    this.problemResponseWriter = problemResponseWriter;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    problemResponseWriter.write(
        response,
        HttpStatus.FORBIDDEN,
        "Access denied",
        "The authenticated account is not allowed to access this endpoint",
        "ACCESS_DENIED");
  }
}
