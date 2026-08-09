package com.kivo.backend.catalog.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.catalog.application.CategoryQueryService;
import com.kivo.backend.catalog.application.query.CategoryResponse;
import com.kivo.backend.common.configuration.WebMvcConfiguration;
import com.kivo.backend.common.exception.handler.GlobalExceptionHandler;
import com.kivo.backend.common.exception.service.MallNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfiguration.class})
class CategoryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CategoryQueryService categoryQueryService;

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
        .thenThrow(new MallNotFoundException("unknown"));

    mockMvc
        .perform(get("/api/v1/malls/unknown/categories"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Ресурс не найден"))
        .andExpect(jsonPath("$.detail").value("Торговый центр с кодом unknown не найден"))
        .andExpect(jsonPath("$.code").value("MALL_NOT_FOUND"));
  }

  @Test
  void localizesProblemDetailsFromAcceptLanguage() throws Exception {
    when(categoryQueryService.findCategories("unknown", "ru"))
        .thenThrow(new MallNotFoundException("unknown"));

    mockMvc
        .perform(
            get("/api/v1/malls/unknown/categories").header(HttpHeaders.ACCEPT_LANGUAGE, "en-US"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource not found"))
        .andExpect(jsonPath("$.detail").value("Mall with code unknown was not found"))
        .andExpect(jsonPath("$.code").value("MALL_NOT_FOUND"));
  }

  @Test
  void rejectsMalformedLanguage() throws Exception {
    mockMvc
        .perform(get("/api/v1/malls/demo-mall/categories").param("lang", "ru-RU"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors").isMap());
  }
}
