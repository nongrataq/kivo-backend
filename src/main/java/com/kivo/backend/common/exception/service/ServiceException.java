package com.kivo.backend.common.exception.service;

import java.util.Map;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;

public abstract class ServiceException extends RuntimeException implements MessageSourceResolvable {

  private final HttpStatus httpStatus;
  private final String exceptionCode;
  private final Map<String, ?> exceptionData;
  private final String messageKey;
  private final transient Object[] messageArgs;

  protected ServiceException(
      HttpStatus httpStatus,
      String exceptionCode,
      Map<String, ?> exceptionData,
      String messageKey,
      Object... messageArgs) {
    super(messageKey);
    this.httpStatus = httpStatus;
    this.exceptionCode = exceptionCode;
    this.exceptionData = exceptionData;
    this.messageKey = messageKey;
    this.messageArgs = messageArgs;
  }

  protected ServiceException(
      HttpStatus httpStatus, String exceptionCode, String messageKey, Object... messageArgs) {
    this(httpStatus, exceptionCode, null, messageKey, messageArgs);
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public String getExceptionCode() {
    return exceptionCode;
  }

  public Map<String, ?> getExceptionData() {
    return exceptionData;
  }

  public String getMessageKey() {
    return messageKey;
  }

  public Object[] getMessageArgs() {
    return messageArgs.clone();
  }

  @Override
  public Object[] getArguments() {
    return getMessageArgs();
  }

  @Override
  public String[] getCodes() {
    return new String[] {messageKey};
  }

  @Override
  public String getDefaultMessage() {
    return messageKey;
  }
}
