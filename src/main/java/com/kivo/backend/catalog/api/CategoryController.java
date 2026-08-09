package com.kivo.backend.catalog.api;

import com.kivo.backend.catalog.application.CategoryQueryService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/malls/{mallCode}/categories")
public class CategoryController {

  private final CategoryQueryService categoryQueryService;

  public CategoryController(CategoryQueryService categoryQueryService) {
    this.categoryQueryService = categoryQueryService;
  }

  @GetMapping
  public List<CategoryResponse> getCategories(
      @PathVariable @Size(min = 1, max = 64) String mallCode,
      @RequestParam(defaultValue = "ru") @Pattern(regexp = "[a-z]{2,3}") String lang) {
    return categoryQueryService.findCategories(mallCode, lang);
  }
}
