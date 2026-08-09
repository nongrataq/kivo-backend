package com.kivo.backend.catalog.api;

import com.kivo.backend.catalog.application.CategoryQueryService;
import com.kivo.backend.catalog.application.query.CategoryResponse;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/malls/{mallCode}/categories")
public class CategoryController {

  private final CategoryQueryService categoryQueryService;

  @GetMapping
  public List<CategoryResponse> getCategories(
      @PathVariable @Size(min = 1, max = 64, message = "{validation.mallCode.size}")
          String mallCode,
      @RequestParam(defaultValue = "ru")
          @Pattern(regexp = "[a-z]{2,3}", message = "{validation.language.pattern}")
          String lang) {
    return categoryQueryService.findCategories(mallCode, lang);
  }
}
