package com.kivo.backend.kiosks.persistence.repository;

import com.kivo.backend.kiosks.persistence.entity.Kiosk;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KioskRepository extends JpaRepository<Kiosk, UUID> {

  @Query(
      """
      select kiosk
      from Kiosk kiosk
      join fetch kiosk.mall mall
      join fetch kiosk.floor floor
      where kiosk.code = :kioskCode
        and kiosk.enabled = true
        and mall.active = true
        and floor.active = true
      """)
  Optional<Kiosk> findActiveByCode(@Param("kioskCode") String kioskCode);
}
