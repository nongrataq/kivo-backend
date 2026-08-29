package com.kivo.backend.feedback.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateFeedbackRequest(
    UUID routeId,
    UUID shareId,
    @NotNull(message = "{validation.feedback.rating.required}")
        @Min(value = 1, message = "{validation.feedback.rating.range}")
        @Max(value = 5, message = "{validation.feedback.rating.range}")
        Integer rating) {

  @AssertTrue(message = "{validation.feedback.context}")
  @JsonIgnore
  public boolean isContextValid() {
    return (routeId == null) != (shareId == null);
  }
}
