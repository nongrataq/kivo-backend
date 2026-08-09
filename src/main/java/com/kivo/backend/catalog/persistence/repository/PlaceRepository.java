package com.kivo.backend.catalog.persistence.repository;

import com.kivo.backend.catalog.persistence.entity.Place;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, UUID> {

  @Query(
      """
      select place
      from Place place
      join fetch place.mall mall
      join fetch place.floor floor
      join fetch place.category category
      where mall.code = :mallCode
        and mall.active = true
        and floor.active = true
        and category.active = true
        and place.active = true
        and (:categoryId is null or category.id = :categoryId)
        and (
          :query is null
          or lower(place.code) like concat('%', lower(:query), '%')
          or exists (
            select translation.id
            from PlaceTranslation translation
            where translation.place = place
              and lower(translation.name) like concat('%', lower(:query), '%')
          )
        )
      order by place.displayOrder, place.code
      """)
  List<Place> searchActive(
      @Param("mallCode") String mallCode,
      @Param("query") String query,
      @Param("categoryId") UUID categoryId,
      Pageable pageable);
}
