package com.kivo.backend.kiosks.persistence.repository;

import com.kivo.backend.kiosks.persistence.entity.Kiosk;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KioskRepository extends JpaRepository<Kiosk, UUID> {}
