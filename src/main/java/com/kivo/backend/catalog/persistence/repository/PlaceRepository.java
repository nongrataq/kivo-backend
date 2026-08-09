package com.kivo.backend.catalog.persistence.repository;

import com.kivo.backend.catalog.persistence.entity.Place;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, UUID> {}
