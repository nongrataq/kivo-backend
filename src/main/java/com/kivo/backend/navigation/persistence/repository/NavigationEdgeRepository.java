package com.kivo.backend.navigation.persistence.repository;

import com.kivo.backend.navigation.persistence.entity.NavigationEdge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NavigationEdgeRepository extends JpaRepository<NavigationEdge, UUID> {

  @Query(
      """
      select distinct edge
      from NavigationEdge edge
      join fetch edge.fromNode
      join fetch edge.toNode
      left join fetch edge.points
      where edge.graphVersion.id = :graphVersionId
      order by edge.externalCode
      """)
  List<NavigationEdge> findAllWithDetailsByGraphVersionId(
      @Param("graphVersionId") UUID graphVersionId);
}
