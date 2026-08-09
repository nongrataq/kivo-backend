package com.kivo.backend.catalog.application;

import com.kivo.backend.catalog.application.query.CategoryMapper;
import com.kivo.backend.catalog.application.query.CategoryResponse;
import com.kivo.backend.catalog.persistence.entity.Category;
import com.kivo.backend.catalog.persistence.entity.CategoryTranslation;
import com.kivo.backend.catalog.persistence.entity.Mall;
import com.kivo.backend.catalog.persistence.repository.CategoryRepository;
import com.kivo.backend.catalog.persistence.repository.MallRepository;
import com.kivo.backend.common.exception.service.MallNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryQueryService {

  private final MallRepository mallRepository;
  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;

  @Transactional(readOnly = true)
  public List<CategoryResponse> findCategories(String mallCode, String requestedLanguage) {
    Mall mall =
        mallRepository
            .findByCodeAndActiveTrue(mallCode)
            .orElseThrow(() -> new MallNotFoundException(mallCode));
    String language = requestedLanguage.toLowerCase(Locale.ROOT);

    return categoryRepository.findActiveByMallCode(mallCode).stream()
        .map(category -> toResponse(category, language, mall.getDefaultLanguage()))
        .toList();
  }

  private CategoryResponse toResponse(Category category, String language, String fallbackLanguage) {
    return findTranslation(category, language)
        .or(() -> findTranslation(category, fallbackLanguage))
        .map(categoryMapper::toCategoryResponse)
        .orElseGet(() -> categoryMapper.toCategoryResponse(category));
  }

  private Optional<CategoryTranslation> findTranslation(Category category, String language) {
    return category.getTranslations().stream()
        .filter(translation -> translation.getLanguage().equalsIgnoreCase(language))
        .findFirst();
  }
}
