package com.kivo.backend.common.api;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail handleNotFound(ResourceNotFoundException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    problem.setTitle("Ресурс не найден");
    problem.setProperty("code", exception.getCode());
    return problem;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    problem.setTitle("Ошибка валидации");
    problem.setProperty("code", "VALIDATION_ERROR");
    return problem;
  }
}
