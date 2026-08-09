package com.kivo.backend.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kivo.backend.common.api.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql("/sql/category-test-data.sql")
class CategoryQueryServiceIntegrationTest {

  @Autowired private CategoryQueryService categoryQueryService;

  @Test
  void returnsActiveCategoriesInDisplayOrder() {
    var categories = categoryQueryService.findCategories("demo-mall", "ru");

    assertThat(categories)
        .extracting(category -> category.code() + ":" + category.name())
        .containsExactly("food:Гастрономия", "fashion:Мода");
  }

  @Test
  void fallsBackToMallDefaultLanguage() {
    var categories = categoryQueryService.findCategories("demo-mall", "en");

    assertThat(categories)
        .extracting(category -> category.code() + ":" + category.name())
        .containsExactly("food:Food", "fashion:Мода");
  }

  @Test
  void rejectsUnknownMall() {
    assertThatThrownBy(() -> categoryQueryService.findCategories("unknown", "ru"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Торговый центр не найден: unknown");
  }
}
