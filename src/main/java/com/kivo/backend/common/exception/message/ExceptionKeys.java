package com.kivo.backend.common.exception.message;

public final class ExceptionKeys {

  public static final String INTERNAL = "exception.internal";
  public static final String CATEGORY_NOT_FOUND_BY_ID_AND_MALL =
      "exception.category.notFoundByIdAndMall";
  public static final String KIOSK_NOT_FOUND_BY_CODE = "exception.kiosk.notFoundByCode";
  public static final String MALL_NOT_FOUND_BY_CODE = "exception.mall.notFoundByCode";
  public static final String PLACE_NOT_FOUND_BY_ID = "exception.place.notFoundById";
  public static final String TITLE_INTERNAL = "exception.title.internal";
  public static final String TITLE_NOT_FOUND = "exception.title.notFound";
  public static final String TITLE_VALIDATION = "exception.title.validation";
  public static final String VALIDATION_DEFAULT = "validation.default";
  public static final String VALIDATION_INVALID_JSON = "validation.invalidJson";
  public static final String VALIDATION_TYPE_MISMATCH = "validation.typeMismatch";

  private ExceptionKeys() {}
}
