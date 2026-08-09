package com.kivo.backend.navigation.persistence.repository;

import com.kivo.backend.navigation.persistence.entity.NavigationGraphVersion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavigationGraphVersionRepository
    extends JpaRepository<NavigationGraphVersion, UUID> {

  Optional<NavigationGraphVersion> findByIdAndMallId(UUID id, UUID mallId);
}
