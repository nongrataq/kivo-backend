package com.kivo.backend.admin.application;

import static com.kivo.backend.common.exception.message.ExceptionKeys.ADMIN_GRAPH_VALIDATION_FAILED;

import com.kivo.backend.common.exception.service.ServiceException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public final class AdminGraphValidationException extends ServiceException {

  public AdminGraphValidationException(int issueCount, Throwable cause) {
    super(
        HttpStatus.UNPROCESSABLE_CONTENT,
        "NAVIGATION_GRAPH_VALIDATION_FAILED",
        Map.of("issueCount", issueCount),
        ADMIN_GRAPH_VALIDATION_FAILED,
        issueCount);
    initCause(cause);
  }
}
