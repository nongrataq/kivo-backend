package com.kivo.backend.navigation.persistence;

import com.kivo.backend.navigation.application.GraphPublicationTransaction;
import com.kivo.backend.navigation.application.GraphVersionNotFoundException;
import com.kivo.backend.navigation.persistence.repository.NavigationGraphPublicationRepository;
import com.kivo.backend.navigation.persistence.repository.NavigationGraphVersionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JpaGraphPublicationTransaction implements GraphPublicationTransaction {

  private final NavigationGraphVersionRepository graphVersionRepository;
  private final NavigationGraphPublicationRepository publicationRepository;

  @Override
  @Transactional
  public void publish(UUID mallId, UUID versionId, Instant publishedAt) {
    var version =
        graphVersionRepository
            .findByIdAndMallId(versionId, mallId)
            .orElseThrow(() -> new GraphVersionNotFoundException(mallId, versionId));
    publicationRepository.ensurePublicationExists(
        UUID.randomUUID(), mallId, versionId, publishedAt);
    var publication =
        publicationRepository
            .findByMallIdForUpdate(mallId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Could not create graph publication for mall " + mallId));
    version.markValidated(publishedAt);
    publication.replaceWith(version, publishedAt);
  }
}
