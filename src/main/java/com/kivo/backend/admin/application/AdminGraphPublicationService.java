package com.kivo.backend.admin.application;

import com.kivo.backend.catalog.application.port.MallIdentityReadPort;
import com.kivo.backend.common.exception.service.MallNotFoundException;
import com.kivo.backend.navigation.application.GraphPublicationService;
import com.kivo.backend.navigation.application.GraphValidationException;
import com.kivo.backend.navigation.application.GraphVersionNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AdminGraphPublicationService {

  private final MallIdentityReadPort mallIdentityReadPort;
  private final GraphPublicationService graphPublicationService;

  public AdminGraphPublicationService(
      MallIdentityReadPort mallIdentityReadPort, GraphPublicationService graphPublicationService) {
    this.mallIdentityReadPort = mallIdentityReadPort;
    this.graphPublicationService = graphPublicationService;
  }

  public PublishedGraph publish(String mallCode, UUID versionId) {
    UUID mallId =
        mallIdentityReadPort
            .findActiveMallIdByCode(mallCode)
            .orElseThrow(() -> new MallNotFoundException(mallCode));
    try {
      var snapshot = graphPublicationService.publish(mallId, versionId);
      return new PublishedGraph(
          mallCode,
          snapshot.versionId(),
          snapshot.versionCode(),
          snapshot.nodes().size(),
          snapshot.edges().size());
    } catch (GraphVersionNotFoundException exception) {
      throw new AdminGraphVersionNotFoundException(mallCode, versionId, exception);
    } catch (GraphValidationException exception) {
      throw new AdminGraphValidationException(
          exception.getValidationResult().issues().size(), exception);
    }
  }
}
