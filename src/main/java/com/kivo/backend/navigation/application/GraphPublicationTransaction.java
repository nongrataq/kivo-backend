package com.kivo.backend.navigation.application;

import java.time.Instant;
import java.util.UUID;

@FunctionalInterface
public interface GraphPublicationTransaction {

  void publish(UUID mallId, UUID versionId, Instant publishedAt);
}
