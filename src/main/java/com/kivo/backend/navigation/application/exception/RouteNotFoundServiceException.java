package com.kivo.backend.navigation.application.exception;

import static com.kivo.backend.common.exception.message.ExceptionKeys.ROUTE_NOT_FOUND;

import com.kivo.backend.common.exception.service.ServiceException;
import org.springframework.http.HttpStatus;

public class RouteNotFoundServiceException extends ServiceException {

  public RouteNotFoundServiceException() {
    super(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND", ROUTE_NOT_FOUND);
  }
}
