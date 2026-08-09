package com.kivo.backend.navigation.persistence.repository;

import com.kivo.backend.navigation.persistence.entity.NavigationNode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavigationNodeRepository extends JpaRepository<NavigationNode, UUID> {

  @EntityGraph(attributePaths = {"floor", "floor.mall"})
  List<NavigationNode> findAllByGraphVersionIdOrderByExternalCode(UUID graphVersionId);
}
