package com.kivo.backend.catalog.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.catalog.application.PlaceQueryService;
import com.kivo.backend.catalog.application.query.PlaceResponse;
import com.kivo.backend.common.configuration.WebMvcConfiguration;
import com.kivo.backend.common.exception.handler.GlobalExceptionHandler;
import com.kivo.backend.common.exception.service.PlaceNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlaceDetailsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfiguration.class})
class PlaceDetailsControllerTest {

  private static final UUID PLACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PlaceQueryService placeQueryService;

  @Test
  void returnsLocalizedPlaceWithoutInternalFields() throws Exception {
    UUID categoryId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    when(placeQueryService.findPlace(PLACE_ID, "en"))
        .thenReturn(
            new PlaceResponse(
                PLACE_ID,
                "grocery",
                "Grocery store",
                "Fresh groceries",
                "L1",
                categoryId,
                "food",
                10));

    mockMvc
        .perform(get("/api/v1/places/{placeId}", PLACE_ID).param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(PLACE_ID.toString()))
        .andExpect(jsonPath("$.name").value("Grocery store"))
        .andExpect(jsonPath("$.navigationNodeCode").doesNotExist())
        .andExpect(jsonPath("$.translations").doesNotExist());

    verify(placeQueryService).findPlace(PLACE_ID, "en");
  }

  @Test
  void rejectsMalformedUuid() throws Exception {
    mockMvc
        .perform(get("/api/v1/places/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.placeId").isArray());
  }

  @Test
  void returnsProblemForUnknownPlace() throws Exception {
    when(placeQueryService.findPlace(PLACE_ID, null))
        .thenThrow(new PlaceNotFoundException(PLACE_ID));

    mockMvc
        .perform(get("/api/v1/places/{placeId}", PLACE_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
  }
}
