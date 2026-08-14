package com.kivo.backend.sharing.application;

import static com.kivo.backend.common.exception.message.ExceptionKeys.SHARED_ROUTE_FLOOR_NOT_FOUND;

import com.kivo.backend.common.exception.service.ServiceException;
import org.springframework.http.HttpStatus;

public class SharedRouteFloorNotFoundException extends ServiceException {

  public SharedRouteFloorNotFoundException(String floorCode) {
    super(
        HttpStatus.NOT_FOUND,
        "SHARED_ROUTE_FLOOR_NOT_FOUND",
        SHARED_ROUTE_FLOOR_NOT_FOUND,
        floorCode);
  }
}
