package com.kivo.backend.feedback.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.common.configuration.WebMvcConfiguration;
import com.kivo.backend.common.exception.handler.GlobalExceptionHandler;
import com.kivo.backend.feedback.application.CreateFeedbackCommand;
import com.kivo.backend.feedback.application.FeedbackContextNotFoundException;
import com.kivo.backend.feedback.application.FeedbackService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfiguration.class})
class FeedbackControllerTest {

  private static final UUID ROUTE_ID = UUID.fromString("73000000-0000-0000-0000-000000000001");
  private static final UUID SHARE_ID = UUID.fromString("74000000-0000-0000-0000-000000000001");

  @Autowired private MockMvc mockMvc;
  @MockitoBean private FeedbackService feedbackService;

  @Test
  void acceptsRouteOrShareFeedbackAndReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"routeId\":\"%s\",\"rating\":5}".formatted(ROUTE_ID)))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(feedbackService).create(new CreateFeedbackCommand(ROUTE_ID, null, 5));
  }

  @Test
  void rejectsMissingDuplicateContextAndRatingsOutsideRange() throws Exception {
    assertValidation("{\"rating\":3}");
    assertValidation(
        "{\"routeId\":\"%s\",\"shareId\":\"%s\",\"rating\":3}".formatted(ROUTE_ID, SHARE_ID));
    assertValidation("{\"shareId\":\"%s\",\"rating\":0}".formatted(SHARE_ID));
    assertValidation("{\"shareId\":\"%s\",\"rating\":6}".formatted(SHARE_ID));
    assertValidation("{\"shareId\":\"%s\"}".formatted(SHARE_ID));
  }

  @Test
  void mapsExpiredOrUnknownContextToStableProblem() throws Exception {
    org.mockito.Mockito.doThrow(new FeedbackContextNotFoundException())
        .when(feedbackService)
        .create(any());

    mockMvc
        .perform(
            post("/api/v1/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"shareId\":\"%s\",\"rating\":4}".formatted(SHARE_ID)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("FEEDBACK_CONTEXT_NOT_FOUND"));
  }

  private void assertValidation(String json) throws Exception {
    mockMvc
        .perform(post("/api/v1/feedback").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
