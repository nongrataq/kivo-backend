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
import com.kivo.backend.common.exception.service.CategoryNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlaceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfiguration.class})
class PlaceControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PlaceQueryService placeQueryService;

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
                    10)));

    mockMvc
        .perform(
            get("/api/v1/malls/demo-mall/places")
                .param("q", "gro")
                .param("categoryId", categoryId.toString())
                .param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(placeId.toString()))
        .andExpect(jsonPath("$[0].code").value("grocery"))
        .andExpect(jsonPath("$[0].name").value("Grocery store"))
        .andExpect(jsonPath("$[0].description").value("Fresh groceries"))
        .andExpect(jsonPath("$[0].floorCode").value("L1"))
        .andExpect(jsonPath("$[0].categoryId").value(categoryId.toString()))
        .andExpect(jsonPath("$[0].categoryCode").value("food"))
        .andExpect(jsonPath("$[0].displayOrder").value(10))
        .andExpect(jsonPath("$[0].navigationNodeCode").doesNotExist())
        .andExpect(jsonPath("$[0].translations").doesNotExist());

    verify(placeQueryService).findPlaces("demo-mall", "gro", categoryId, "en");
  }

  @Test
  void rejectsMalformedCategoryId() throws Exception {
    mockMvc
        .perform(get("/api/v1/malls/demo-mall/places").param("categoryId", "not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.categoryId").isArray());
  }

  @Test
  void rejectsOversizedQuery() throws Exception {
    mockMvc
        .perform(get("/api/v1/malls/demo-mall/places").param("q", "x".repeat(101)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void returnsProblemWhenCategoryDoesNotBelongToMall() throws Exception {
    UUID categoryId = UUID.fromString("00000000-0000-0000-0000-000000000801");
    when(placeQueryService.findPlaces("demo-mall", null, categoryId, null))
        .thenThrow(new CategoryNotFoundException(categoryId, "demo-mall"));

    mockMvc
        .perform(get("/api/v1/malls/demo-mall/places").param("categoryId", categoryId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
  }
}
