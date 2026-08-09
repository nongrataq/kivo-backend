package com.kivo.backend.catalog.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.catalog.application.PlaceQueryService;
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
    mockMvc = MockMvcBuilders.standaloneSetup(new PlaceController(placeQueryService)).build();
  }

  @Test
  void returnsFlatLocalizedPlacesAndPassesFilters() throws Exception {
    UUID placeId = UUID.fromString("00000000-0000-0000-0000-000000000401");
    UUID categoryId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    when(placeQueryService.findPlaces("demo-mall", "gro", categoryId, "en"))
        .thenReturn(
            List.of(
                new PlaceResponse(
                    placeId,
                    "grocery",
                    "Grocery store",
                    "Fresh groceries",
                    "L1",
                    categoryId,
                    "food",
                    "node-grocery",
                    10)));

    mockMvc
        .perform(
            get("/api/v1/malls/demo-mall/places")
                .param("q", "gro")
                .param("categoryId", categoryId.toString())
                .param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(placeId.toString()))
        .andExpect(jsonPath("$[0].name").value("Grocery store"))
        .andExpect(jsonPath("$[0].floorCode").value("L1"))
        .andExpect(jsonPath("$[0].categoryCode").value("food"))
        .andExpect(jsonPath("$[0].navigationNodeCode").value("node-grocery"));

    verify(placeQueryService).findPlaces("demo-mall", "gro", categoryId, "en");
  }
}
