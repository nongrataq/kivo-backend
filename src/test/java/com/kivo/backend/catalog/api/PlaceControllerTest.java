package com.kivo.backend.catalog.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.catalog.application.PlaceQueryService;
import com.kivo.backend.common.api.ApiExceptionHandler;
import com.kivo.backend.common.api.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PlaceControllerTest {

  private PlaceQueryService placeQueryService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    placeQueryService = org.mockito.Mockito.mock(PlaceQueryService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new PlaceController(placeQueryService))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
  }

  @Test
  void returnsFlatFilteredJsonArray() throws Exception {
    UUID placeId = UUID.fromString("00000000-0000-0000-0000-000000000401");
    UUID categoryId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    when(placeQueryService.findPlaces("demo-mall", "coffee", categoryId, "en"))
        .thenReturn(
            List.of(
                new PlaceResponse(
                    placeId,
                    "coffee-shop",
                    "Coffee shop",
                    "Coffee and desserts",
                    categoryId,
                    "ground",
                    "node-coffee",
                    10)));

    mockMvc
        .perform(
            get("/api/v1/malls/demo-mall/places")
                .param("q", "coffee")
                .param("categoryId", categoryId.toString())
                .param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(placeId.toString()))
        .andExpect(jsonPath("$[0].code").value("coffee-shop"))
        .andExpect(jsonPath("$[0].name").value("Coffee shop"))
        .andExpect(jsonPath("$[0].description").value("Coffee and desserts"))
        .andExpect(jsonPath("$[0].categoryId").value(categoryId.toString()))
        .andExpect(jsonPath("$[0].floorCode").value("ground"))
        .andExpect(jsonPath("$[0].navigationNodeCode").value("node-coffee"))
        .andExpect(jsonPath("$[0].displayOrder").value(10))
        .andExpect(jsonPath("$[0].translations").doesNotExist());
  }

  @Test
  void returnsProblemDetailsForUnknownMall() throws Exception {
    when(placeQueryService.findPlaces("unknown", null, null, "ru"))
        .thenThrow(
            new ResourceNotFoundException("MALL_NOT_FOUND", "Торговый центр не найден: unknown"));

    mockMvc
        .perform(get("/api/v1/malls/unknown/places"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Ресурс не найден"))
        .andExpect(jsonPath("$.code").value("MALL_NOT_FOUND"));
  }
}
