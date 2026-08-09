package com.kivo.backend.common.exception.service;

import static com.kivo.backend.common.exception.message.ExceptionKeys.MALL_NOT_FOUND_BY_CODE;

import org.springframework.http.HttpStatus;

public class MallNotFoundException extends ServiceException {

  public MallNotFoundException(String mallCode) {
    super(HttpStatus.NOT_FOUND, "MALL_NOT_FOUND", MALL_NOT_FOUND_BY_CODE, mallCode);
  }
}
