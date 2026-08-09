package com.kivo.backend.catalog.application.query;

import com.kivo.backend.catalog.persistence.entity.Category;
import com.kivo.backend.catalog.persistence.entity.CategoryTranslation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface CategoryMapper {

  @Mapping(source = "category.id", target = "id")
  @Mapping(source = "category.code", target = "code")
  @Mapping(source = "category.displayOrder", target = "displayOrder")
  CategoryResponse toCategoryResponse(CategoryTranslation categoryTranslation);

  @Mapping(source = "code", target = "name")
  CategoryResponse toCategoryResponse(Category category);
}
