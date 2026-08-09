package com.kivo.backend.navigation.application;

import com.kivo.backend.navigation.domain.GraphValidator;
import com.kivo.backend.navigation.persistence.entity.NavigationGraphPublication;
import com.kivo.backend.navigation.persistence.repository.NavigationGraphPublicationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveGraphRestorer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(ActiveGraphRestorer.class);

  private final NavigationGraphPublicationRepository publicationRepository;
  private final GraphSnapshotLoader snapshotLoader;
  private final GraphValidator graphValidator;
  private final InMemoryActiveGraphRegistry activeGraphRegistry;

  @Override
  public void run(ApplicationArguments args) {
    restoreAll();
  }

  public void restoreAll() {
    publicationRepository.findAllByOrderByPublishedAtAsc().forEach(this::restore);
  }

  private void restore(NavigationGraphPublication publication) {
    var mallId = publication.getMall().getId();
    var versionId = publication.getActiveGraphVersion().getId();
    var snapshot = snapshotLoader.load(mallId, versionId);
    var validationResult = graphValidator.validate(snapshot);
    if (!validationResult.isValid()) {
      throw new GraphValidationException(validationResult);
    }
    activeGraphRegistry.replace(snapshot);
    log.atInfo()
        .addKeyValue("mallId", mallId)
        .addKeyValue("versionId", versionId)
        .log("Active navigation graph restored");
  }
}
