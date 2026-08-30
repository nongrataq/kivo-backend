package com.kivo.backend.events.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.common.configuration.WebMvcConfiguration;
import com.kivo.backend.common.exception.handler.GlobalExceptionHandler;
import com.kivo.backend.common.exception.service.MallNotFoundException;
import com.kivo.backend.events.application.EventQueryService;
import com.kivo.backend.events.application.EventResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfiguration.class})
class EventControllerTest {

  private static final UUID EVENT_ID = UUID.fromString("75000000-0000-0000-0000-000000000001");
  private static final UUID PLACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

  @Autowired private MockMvc mockMvc;
  @MockitoBean private EventQueryService eventQueryService;

  @Test
  void returnsFlatLocalizedEvents() throws Exception {
    when(eventQueryService.findToday("demo-mall", "ru"))
        .thenReturn(
            List.of(
                new EventResponse(
                    EVENT_ID,
                    "tasting",
                    "Дегустация",
                    "Новинки фермерских продуктов",
                    Instant.parse("2026-08-10T10:00:00Z"),
                    Instant.parse("2026-08-10T11:00:00Z"),
                    PLACE_ID,
                    "grocery",
                    "Продуктовый магазин",
                    "L1")));

    mockMvc
        .perform(get("/api/v1/malls/demo-mall/events/today").queryParam("lang", "ru"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$[0].title").value("Дегустация"))
        .andExpect(jsonPath("$[0].placeId").value(PLACE_ID.toString()))
        .andExpect(jsonPath("$[0].floorCode").value("L1"))
        .andExpect(jsonPath("$[0].translations").doesNotExist());
  }

  @Test
  void validatesLanguageAndMapsUnknownMall() throws Exception {
    mockMvc
        .perform(get("/api/v1/malls/demo-mall/events/today").queryParam("lang", "RU"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    when(eventQueryService.findToday("unknown", null))
        .thenThrow(new MallNotFoundException("unknown"));
    mockMvc
        .perform(get("/api/v1/malls/unknown/events/today"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MALL_NOT_FOUND"));
  }
}
