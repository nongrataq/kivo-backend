package com.kivo.backend.catalog.persistence.repository;

import com.kivo.backend.catalog.persistence.entity.Mall;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MallRepository extends JpaRepository<Mall, UUID> {

  Optional<Mall> findByCodeAndActiveTrue(String code);
}
