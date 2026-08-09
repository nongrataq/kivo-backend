package com.kivo.backend.navigation.application;

import com.kivo.backend.navigation.domain.GraphValidator;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultGraphPublicationService implements GraphPublicationService {

  private static final Logger log = LoggerFactory.getLogger(DefaultGraphPublicationService.class);
  private static final String PUBLICATION_COUNTER = "kivo.navigation.graph.publication";
  private static final String PUBLICATION_TIMER = "kivo.navigation.graph.publication.duration";

  private final GraphSnapshotLoader snapshotLoader;
  private final GraphValidator graphValidator;
  private final GraphPublicationTransaction publicationTransaction;
  private final InMemoryActiveGraphRegistry activeGraphRegistry;
  private final MeterRegistry meterRegistry;
  private final Clock clock;

  public DefaultGraphPublicationService(
      GraphSnapshotLoader snapshotLoader,
      GraphValidator graphValidator,
      GraphPublicationTransaction publicationTransaction,
      InMemoryActiveGraphRegistry activeGraphRegistry,
      MeterRegistry meterRegistry,
      Clock clock) {
    this.snapshotLoader = snapshotLoader;
    this.graphValidator = graphValidator;
    this.publicationTransaction = publicationTransaction;
    this.activeGraphRegistry = activeGraphRegistry;
    this.meterRegistry = meterRegistry;
    this.clock = clock;
  }

  @Override
  public GraphSnapshot publish(UUID mallId, UUID versionId) {
    long startedAt = System.nanoTime();
    String outcome = "error";
    try {
      GraphSnapshot candidate = snapshotLoader.load(mallId, versionId);
      var validationResult = graphValidator.validate(candidate);
      if (!validationResult.isValid()) {
        outcome = "rejected";
        throw new GraphValidationException(validationResult);
      }

      Instant publishedAt = clock.instant();
      publicationTransaction.publish(mallId, versionId, publishedAt);
      activeGraphRegistry.replace(candidate);
      outcome = "success";
      log.atInfo()
          .addKeyValue("mallId", mallId)
          .addKeyValue("versionId", versionId)
          .addKeyValue("nodes", candidate.nodes().size())
          .addKeyValue("edges", candidate.edges().size())
          .log("Navigation graph published");
      return candidate;
    } catch (GraphValidationException exception) {
      log.atWarn()
          .addKeyValue("mallId", mallId)
          .addKeyValue("versionId", versionId)
          .addKeyValue("issues", exception.getValidationResult().issues().size())
          .log("Navigation graph publication rejected");
      throw exception;
    } catch (RuntimeException exception) {
      log.atError()
          .addKeyValue("mallId", mallId)
          .addKeyValue("versionId", versionId)
          .setCause(exception)
          .log("Navigation graph publication failed");
      throw exception;
    } finally {
      recordMetrics(outcome, System.nanoTime() - startedAt);
    }
  }

  private void recordMetrics(String outcome, long elapsedNanos) {
    meterRegistry.counter(PUBLICATION_COUNTER, "outcome", outcome).increment();
    Timer.builder(PUBLICATION_TIMER)
        .tag("outcome", outcome)
        .register(meterRegistry)
        .record(Duration.ofNanos(elapsedNanos));
  }
}
