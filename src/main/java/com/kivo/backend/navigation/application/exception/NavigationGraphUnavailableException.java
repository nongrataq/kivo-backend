package com.kivo.backend.navigation.application.exception;

import static com.kivo.backend.common.exception.message.ExceptionKeys.NAVIGATION_GRAPH_UNAVAILABLE;

import com.kivo.backend.common.exception.service.ServiceException;
import org.springframework.http.HttpStatus;

public class NavigationGraphUnavailableException extends ServiceException {

  public NavigationGraphUnavailableException() {
    super(
        HttpStatus.SERVICE_UNAVAILABLE,
        "NAVIGATION_GRAPH_UNAVAILABLE",
        NAVIGATION_GRAPH_UNAVAILABLE);
  }
}
