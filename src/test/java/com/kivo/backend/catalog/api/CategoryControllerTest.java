package com.kivo.backend.catalog.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.catalog.application.CategoryQueryService;
import com.kivo.backend.common.api.ApiExceptionHandler;
import com.kivo.backend.common.api.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CategoryControllerTest {

  private CategoryQueryService categoryQueryService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    categoryQueryService = org.mockito.Mockito.mock(CategoryQueryService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new CategoryController(categoryQueryService))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
  }

  @Test
  void returnsFlatJsonArray() throws Exception {
    UUID categoryId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    when(categoryQueryService.findCategories("demo-mall", "ru"))
        .thenReturn(List.of(new CategoryResponse(categoryId, "food", "Гастрономия", 10)));

    mockMvc
        .perform(get("/api/v1/malls/demo-mall/categories").param("lang", "ru"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(categoryId.toString()))
        .andExpect(jsonPath("$[0].code").value("food"))
        .andExpect(jsonPath("$[0].name").value("Гастрономия"))
        .andExpect(jsonPath("$[0].displayOrder").value(10));
  }

  @Test
  void returnsProblemDetailsForUnknownMall() throws Exception {
    when(categoryQueryService.findCategories("unknown", "ru"))
        .thenThrow(
            new ResourceNotFoundException("MALL_NOT_FOUND", "Торговый центр не найден: unknown"));

    mockMvc
        .perform(get("/api/v1/malls/unknown/categories"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Ресурс не найден"))
        .andExpect(jsonPath("$.code").value("MALL_NOT_FOUND"));
  }
}
