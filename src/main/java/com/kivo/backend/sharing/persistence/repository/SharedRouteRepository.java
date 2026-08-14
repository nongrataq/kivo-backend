package com.kivo.backend.sharing.persistence.repository;

import com.kivo.backend.sharing.persistence.entity.SharedRoute;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedRouteRepository extends JpaRepository<SharedRoute, UUID> {

  Optional<SharedRoute> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
      String tokenHash, Instant now);
}
