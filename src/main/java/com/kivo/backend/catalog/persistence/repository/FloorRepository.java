package com.kivo.backend.catalog.persistence.repository;

import com.kivo.backend.catalog.persistence.entity.Floor;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<Floor, UUID> {

  List<Floor> findByMallIdAndActiveTrueOrderByDisplayOrderAscCodeAsc(UUID mallId);
}
