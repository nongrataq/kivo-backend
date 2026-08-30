package com.kivo.backend.common.exception.handler;

import static com.kivo.backend.common.exception.message.ExceptionKeys.INTERNAL;
import static com.kivo.backend.common.exception.message.ExceptionKeys.TITLE_INTERNAL;
import static com.kivo.backend.common.exception.message.ExceptionKeys.TITLE_NOT_FOUND;
import static com.kivo.backend.common.exception.message.ExceptionKeys.TITLE_UNAVAILABLE;
import static com.kivo.backend.common.exception.message.ExceptionKeys.TITLE_UNPROCESSABLE;
import static com.kivo.backend.common.exception.message.ExceptionKeys.TITLE_VALIDATION;
import static com.kivo.backend.common.exception.message.ExceptionKeys.VALIDATION_DEFAULT;
import static com.kivo.backend.common.exception.message.ExceptionKeys.VALIDATION_INVALID_JSON;
import static com.kivo.backend.common.exception.message.ExceptionKeys.VALIDATION_TYPE_MISMATCH;

import com.kivo.backend.common.exception.service.ServiceException;
import com.kivo.backend.common.web.CorrelationIdFilter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  private final MessageSource exceptionsMessageSource;
  private final MessageSource validationMessageSource;

  public GlobalExceptionHandler(
      @Qualifier("exceptionsMessageSource") MessageSource exceptionsMessageSource,
      @Qualifier("validationMessageSource") MessageSource validationMessageSource) {
    this.exceptionsMessageSource = exceptionsMessageSource;
    this.validationMessageSource = validationMessageSource;
  }

  @ExceptionHandler(ServiceException.class)
  ResponseEntity<ProblemDetail> handleServiceException(ServiceException exception) {
    log.warn("Caught service exception: {}", exception.getMessageKey());
    Locale locale = LocaleContextHolder.getLocale();
    String detail =
        exceptionsMessageSource.getMessage(
            exception.getMessageKey(),
            exception.getMessageArgs(),
            exception.getMessageKey(),
            locale);
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getHttpStatus(), detail);
    problem.setTitle(titleFor(exception.getHttpStatus(), locale));
    problem.setProperty("code", exception.getExceptionCode());
    addCorrelationId(problem);
    if (exception.getExceptionData() != null) {
      problem.setProperty("data", exception.getExceptionData());
    }
    return ResponseEntity.status(exception.getHttpStatus()).body(problem);
  }

  @ExceptionHandler(BindException.class)
  ResponseEntity<ProblemDetail> handleBindException(BindException exception) {
    Map<String, List<String>> errors =
        exception.getBindingResult().getAllErrors().stream()
            .collect(
                Collectors.groupingBy(
                    error ->
                        error instanceof FieldError fieldError ? fieldError.getField() : "request",
                    Collectors.mapping(
                        error ->
                            validationMessageSource.getMessage(
                                messageKey(error.getDefaultMessage()),
                                error.getArguments(),
                                VALIDATION_DEFAULT,
                                LocaleContextHolder.getLocale()),
                        Collectors.toList())));
    return validationProblem(errors);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
    String requiredType =
        exception.getRequiredType() == null
            ? "unknown"
            : exception.getRequiredType().getSimpleName();
    String message =
        Objects.requireNonNull(
            validationMessageSource.getMessage(
                VALIDATION_TYPE_MISMATCH,
                new Object[] {String.valueOf(exception.getValue()), requiredType},
                VALIDATION_TYPE_MISMATCH,
                LocaleContextHolder.getLocale()));
    return validationProblem(Map.of(exception.getName(), List.of(message)));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ProblemDetail> handleUnreadableMessage(HttpMessageNotReadableException exception) {
    log.warn("Failed to read HTTP message: {}", exception.getMessage());
    String message =
        Objects.requireNonNull(
            validationMessageSource.getMessage(
                VALIDATION_INVALID_JSON,
                null,
                VALIDATION_INVALID_JSON,
                LocaleContextHolder.getLocale()));
    return validationProblem(Map.of("requestBody", List.of(message)));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
    Map<String, List<String>> errors =
        exception.getConstraintViolations().stream()
            .collect(
                Collectors.groupingBy(
                    violation -> violation.getPropertyPath().toString(),
                    Collectors.mapping(this::localizedConstraintMessage, Collectors.toList())));
    return validationProblem(errors);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> handleUnexpectedException(Exception exception) {
    log.error("Caught unexpected exception", exception);
    Locale locale = LocaleContextHolder.getLocale();
    String detail = exceptionsMessageSource.getMessage(INTERNAL, null, INTERNAL, locale);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, detail);
    problem.setTitle(
        exceptionsMessageSource.getMessage(TITLE_INTERNAL, null, TITLE_INTERNAL, locale));
    problem.setProperty("code", "INTERNAL_ERROR");
    addCorrelationId(problem);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  private ResponseEntity<ProblemDetail> validationProblem(Map<String, List<String>> errors) {
    Locale locale = LocaleContextHolder.getLocale();
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle(
        exceptionsMessageSource.getMessage(TITLE_VALIDATION, null, TITLE_VALIDATION, locale));
    problem.setProperty("code", "VALIDATION_ERROR");
    addCorrelationId(problem);
    problem.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problem);
  }

  private String localizedConstraintMessage(ConstraintViolation<?> violation) {
    String key = messageKey(violation.getMessage());
    return validationMessageSource.getMessage(
        key, null, violation.getMessage(), LocaleContextHolder.getLocale());
  }

  private String titleFor(HttpStatus status, Locale locale) {
    String key =
        switch (status) {
          case NOT_FOUND -> TITLE_NOT_FOUND;
          case SERVICE_UNAVAILABLE -> TITLE_UNAVAILABLE;
          case UNPROCESSABLE_CONTENT -> TITLE_UNPROCESSABLE;
          default -> status.getReasonPhrase();
        };
    return exceptionsMessageSource.getMessage(key, null, key, locale);
  }

  private String messageKey(String message) {
    if (message == null || message.isBlank()) {
      return VALIDATION_DEFAULT;
    }
    if (message.startsWith("{") && message.endsWith("}")) {
      return message.substring(1, message.length() - 1);
    }
    return message;
  }

  private void addCorrelationId(ProblemDetail problem) {
    String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
    if (correlationId != null) {
      problem.setProperty("correlationId", correlationId);
    }
  }
}
