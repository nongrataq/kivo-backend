package com.kivo.backend.common.exception.service;

import static com.kivo.backend.common.exception.message.ExceptionKeys.KIOSK_NOT_FOUND_BY_CODE;

import org.springframework.http.HttpStatus;

public class KioskNotFoundException extends ServiceException {

  public KioskNotFoundException(String kioskCode) {
    super(HttpStatus.NOT_FOUND, "KIOSK_NOT_FOUND", KIOSK_NOT_FOUND_BY_CODE, kioskCode);
  }
}
