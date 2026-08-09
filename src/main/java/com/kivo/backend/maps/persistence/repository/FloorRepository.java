package com.kivo.backend.maps.persistence.repository;

import com.kivo.backend.maps.persistence.entity.Floor;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<Floor, UUID> {}
