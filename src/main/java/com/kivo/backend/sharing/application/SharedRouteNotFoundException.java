package com.kivo.backend.sharing.application;

import static com.kivo.backend.common.exception.message.ExceptionKeys.SHARED_ROUTE_NOT_FOUND;

import com.kivo.backend.common.exception.service.ServiceException;
import org.springframework.http.HttpStatus;

public class SharedRouteNotFoundException extends ServiceException {

  public SharedRouteNotFoundException() {
    super(HttpStatus.NOT_FOUND, "SHARED_ROUTE_NOT_FOUND", SHARED_ROUTE_NOT_FOUND);
  }
}
