package com.kivo.backend.catalog.application.query;

import com.kivo.backend.catalog.persistence.entity.Place;
import com.kivo.backend.catalog.persistence.entity.PlaceTranslation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface PlaceMapper {

  @Mapping(source = "place.id", target = "id")
  @Mapping(source = "place.code", target = "code")
  @Mapping(source = "place.floor.code", target = "floorCode")
  @Mapping(source = "place.category.id", target = "categoryId")
  @Mapping(source = "place.category.code", target = "categoryCode")
  @Mapping(source = "place.displayOrder", target = "displayOrder")
  PlaceResponse toPlaceResponse(PlaceTranslation placeTranslation);

  @Mapping(source = "code", target = "name")
  @Mapping(target = "description", ignore = true)
  @Mapping(source = "floor.code", target = "floorCode")
  @Mapping(source = "category.id", target = "categoryId")
  @Mapping(source = "category.code", target = "categoryCode")
  PlaceResponse toPlaceResponse(Place place);
}
