package com.kivo.backend.catalog.application;

import com.kivo.backend.catalog.api.CategoryResponse;
import com.kivo.backend.catalog.persistence.entity.Category;
import com.kivo.backend.catalog.persistence.entity.CategoryTranslation;
import com.kivo.backend.catalog.persistence.entity.Mall;
import com.kivo.backend.catalog.persistence.repository.CategoryRepository;
import com.kivo.backend.catalog.persistence.repository.MallRepository;
import com.kivo.backend.common.api.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryQueryService {

  private final MallRepository mallRepository;
  private final CategoryRepository categoryRepository;

  public CategoryQueryService(
      MallRepository mallRepository, CategoryRepository categoryRepository) {
    this.mallRepository = mallRepository;
    this.categoryRepository = categoryRepository;
  }

  @Transactional(readOnly = true)
  public List<CategoryResponse> findCategories(String mallCode, String requestedLanguage) {
    Mall mall =
        mallRepository
            .findByCodeAndActiveTrue(mallCode)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "MALL_NOT_FOUND", "Торговый центр не найден: " + mallCode));
    String language = requestedLanguage.toLowerCase(Locale.ROOT);

    return categoryRepository.findActiveByMallCode(mallCode).stream()
        .map(category -> toResponse(category, language, mall.getDefaultLanguage()))
        .toList();
  }

  private CategoryResponse toResponse(Category category, String language, String fallbackLanguage) {
    String name =
        findName(category, language)
            .or(() -> findName(category, fallbackLanguage))
            .orElse(category.getCode());
    return new CategoryResponse(
        category.getId(), category.getCode(), name, category.getDisplayOrder());
  }

  private Optional<String> findName(Category category, String language) {
    return category.getTranslations().stream()
        .filter(translation -> translation.getLanguage().equalsIgnoreCase(language))
        .map(CategoryTranslation::getName)
        .findFirst();
  }
}
