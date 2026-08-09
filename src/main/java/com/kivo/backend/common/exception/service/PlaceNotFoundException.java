package com.kivo.backend.common.exception.service;

import static com.kivo.backend.common.exception.message.ExceptionKeys.PLACE_NOT_FOUND_BY_ID;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class PlaceNotFoundException extends ServiceException {

  public PlaceNotFoundException(UUID placeId) {
    super(HttpStatus.NOT_FOUND, "PLACE_NOT_FOUND", PLACE_NOT_FOUND_BY_ID, placeId);
  }
}
