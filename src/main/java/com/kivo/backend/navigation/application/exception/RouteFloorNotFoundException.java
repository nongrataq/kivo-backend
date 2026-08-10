package com.kivo.backend.navigation.application.exception;

import static com.kivo.backend.common.exception.message.ExceptionKeys.ROUTE_FLOOR_NOT_FOUND;

import com.kivo.backend.common.exception.service.ServiceException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class RouteFloorNotFoundException extends ServiceException {

  public RouteFloorNotFoundException(UUID routeId, String floorCode) {
    super(HttpStatus.NOT_FOUND, "ROUTE_FLOOR_NOT_FOUND", ROUTE_FLOOR_NOT_FOUND, routeId, floorCode);
  }
}
