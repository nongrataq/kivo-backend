package com.kivo.backend.navigation.persistence.repository;

import com.kivo.backend.navigation.persistence.entity.NavigationGraphPublication;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NavigationGraphPublicationRepository
    extends JpaRepository<NavigationGraphPublication, UUID> {

  @Modifying
  @Query(
      value =
          """
          insert into navigation_graph_publication
            (id, mall_id, active_graph_version_id, published_at)
          values (:id, :mallId, :versionId, :publishedAt)
          on conflict (mall_id) do nothing
          """,
      nativeQuery = true)
  int ensurePublicationExists(
      @Param("id") UUID id,
      @Param("mallId") UUID mallId,
      @Param("versionId") UUID versionId,
      @Param("publishedAt") Instant publishedAt);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select publication
      from NavigationGraphPublication publication
      where publication.mall.id = :mallId
      """)
  Optional<NavigationGraphPublication> findByMallIdForUpdate(@Param("mallId") UUID mallId);

  @EntityGraph(attributePaths = {"mall", "activeGraphVersion"})
  List<NavigationGraphPublication> findAllByOrderByPublishedAtAsc();
}
