package com.kivo.backend.feedback.api;

import com.kivo.backend.feedback.application.CreateFeedbackCommand;
import com.kivo.backend.feedback.application.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

  private final FeedbackService feedbackService;

  @PostMapping
  public ResponseEntity<Void> create(@Valid @RequestBody CreateFeedbackRequest request) {
    feedbackService.create(
        new CreateFeedbackCommand(request.routeId(), request.shareId(), request.rating()));
    return ResponseEntity.noContent().build();
  }
}
