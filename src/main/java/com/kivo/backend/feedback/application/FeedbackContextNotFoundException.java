package com.kivo.backend.feedback.application;

import com.kivo.backend.common.exception.service.ServiceException;
import org.springframework.http.HttpStatus;

public class FeedbackContextNotFoundException extends ServiceException {

  public FeedbackContextNotFoundException() {
    super(HttpStatus.NOT_FOUND, "FEEDBACK_CONTEXT_NOT_FOUND", "exception.feedback.contextNotFound");
  }
}
