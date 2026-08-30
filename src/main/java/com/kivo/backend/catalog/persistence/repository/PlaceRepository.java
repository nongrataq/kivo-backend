package com.kivo.backend.catalog.persistence.repository;

import com.kivo.backend.catalog.application.port.RoutablePlaceReadModel;
import com.kivo.backend.catalog.persistence.entity.Place;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, UUID> {

  @Query(
      """
      select new com.kivo.backend.catalog.application.port.RoutablePlaceReadModel(
        place.code, place.navigationNodeCode)
      from Place place
      join place.mall mall
      join place.floor floor
      join place.category category
      where mall.id = :mallId
        and mall.active = true
        and floor.active = true
        and category.active = true
        and place.active = true
      order by place.displayOrder, place.code
      """)
  List<RoutablePlaceReadModel> findActiveRoutablePlaces(@Param("mallId") UUID mallId);

  @Query(
      """
      select place.id
      from Place place
      join place.mall mall
      join place.floor floor
      join place.category category
      where mall.code = :mallCode
        and mall.active = true
        and floor.active = true
        and category.active = true
        and place.active = true
        and (:categoryId is null or category.id = :categoryId)
        and (
          lower(place.code) like concat('%', lower(:query), '%')
          or exists (
            select translation.id
            from PlaceTranslation translation
            where translation.place = place
              and (
                lower(translation.name) like concat('%', lower(:query), '%')
                or lower(translation.description) like concat('%', lower(:query), '%')
              )
          )
        )
      order by place.displayOrder, place.code
      """)
  List<UUID> searchActiveIds(
      @Param("mallCode") String mallCode,
      @Param("query") String query,
      @Param("categoryId") UUID categoryId,
      Pageable pageable);

  @Query(
      """
      select place.id
      from Place place
      join place.mall mall
      join place.floor floor
      join place.category category
      where mall.code = :mallCode
        and mall.active = true
        and floor.active = true
        and category.active = true
        and place.active = true
        and (:categoryId is null or category.id = :categoryId)
      order by place.displayOrder, place.code
      """)
  List<UUID> findActiveIds(
      @Param("mallCode") String mallCode, @Param("categoryId") UUID categoryId, Pageable pageable);

  @Query(
      """
      select distinct place
      from Place place
      join fetch place.floor floor
      join fetch place.category category
      left join fetch place.translations translation
      where place.id in :ids
      order by place.displayOrder, place.code
      """)
  List<Place> findAllWithDetailsByIdIn(@Param("ids") Collection<UUID> ids);

  @Query(
      """
      select distinct place
      from Place place
      join fetch place.mall mall
      join fetch place.floor floor
      join fetch place.category category
      left join fetch place.translations translation
      where place.id = :placeId
        and place.active = true
        and mall.active = true
        and floor.active = true
        and category.active = true
      """)
  Optional<Place> findActiveWithDetailsById(@Param("placeId") UUID placeId);

  @Query(
      """
      select distinct place
      from Place place
      join fetch place.mall mall
      join fetch place.floor floor
      join fetch place.category category
      left join fetch place.translations translation
      where mall.id = :mallId
        and mall.active = true
        and floor.active = true
        and category.active = true
        and place.active = true
        and place.navigationNodeCode is not null
        and place.navigationNodeCode <> ''
      order by place.displayOrder, place.code
      """)
  List<Place> findActiveRoutableWithDetailsByMallId(@Param("mallId") UUID mallId);
}
