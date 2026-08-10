package com.kivo.backend.navigation.application.exception;

import static com.kivo.backend.common.exception.message.ExceptionKeys.ROUTE_ENDPOINT_MISMATCH;

import com.kivo.backend.common.exception.service.ServiceException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class RouteEndpointMismatchException extends ServiceException {

  public RouteEndpointMismatchException(String kioskCode, UUID placeId) {
    super(
        HttpStatus.UNPROCESSABLE_CONTENT,
        "ROUTE_ENDPOINT_MISMATCH",
        ROUTE_ENDPOINT_MISMATCH,
        kioskCode,
        placeId);
  }
}
