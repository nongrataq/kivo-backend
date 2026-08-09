package com.kivo.backend.common.exception.service;

import static com.kivo.backend.common.exception.message.ExceptionKeys.CATEGORY_NOT_FOUND_BY_ID_AND_MALL;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CategoryNotFoundException extends ServiceException {

  public CategoryNotFoundException(UUID categoryId, String mallCode) {
    super(
        HttpStatus.NOT_FOUND,
        "CATEGORY_NOT_FOUND",
        CATEGORY_NOT_FOUND_BY_ID_AND_MALL,
        categoryId,
        mallCode);
  }
}
