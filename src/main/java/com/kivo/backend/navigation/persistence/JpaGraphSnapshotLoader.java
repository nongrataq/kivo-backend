package com.kivo.backend.navigation.persistence;

import com.kivo.backend.catalog.application.port.CatalogNavigationReadPort;
import com.kivo.backend.catalog.application.port.RoutablePlaceReadModel;
import com.kivo.backend.kiosks.application.port.KioskNavigationReadPort;
import com.kivo.backend.kiosks.application.port.KioskStartReadModel;
import com.kivo.backend.navigation.application.GraphSnapshotLoader;
import com.kivo.backend.navigation.application.GraphVersionNotFoundException;
import com.kivo.backend.navigation.domain.model.GraphAnchor;
import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphPoint;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import com.kivo.backend.navigation.persistence.entity.NavigationEdgePoint;
import com.kivo.backend.navigation.persistence.repository.NavigationEdgeRepository;
import com.kivo.backend.navigation.persistence.repository.NavigationGraphVersionRepository;
import com.kivo.backend.navigation.persistence.repository.NavigationNodeRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JpaGraphSnapshotLoader implements GraphSnapshotLoader {

  private final NavigationGraphVersionRepository graphVersionRepository;
  private final NavigationNodeRepository nodeRepository;
  private final NavigationEdgeRepository edgeRepository;
  private final CatalogNavigationReadPort catalogNavigationReadPort;
  private final KioskNavigationReadPort kioskNavigationReadPort;

  @Override
  @Transactional(readOnly = true)
  public GraphSnapshot load(UUID mallId, UUID versionId) {
    var version =
        graphVersionRepository
            .findByIdAndMallId(versionId, mallId)
            .orElseThrow(() -> new GraphVersionNotFoundException(mallId, versionId));
    var nodes =
        nodeRepository.findAllByGraphVersionIdOrderByExternalCode(versionId).stream()
            .map(
                node ->
                    new GraphNode(
                        node.getExternalCode(),
                        node.getFloor().getId(),
                        node.getFloor().getMall().getId(),
                        node.getFloor().getCoordinateWidth(),
                        node.getFloor().getCoordinateHeight(),
                        node.getX(),
                        node.getY(),
                        node.getNodeType()))
            .toList();
    var edges =
        edgeRepository.findAllWithDetailsByGraphVersionId(versionId).stream()
            .map(
                edge ->
                    new GraphEdge(
                        edge.getExternalCode(),
                        edge.getFromNode().getExternalCode(),
                        edge.getToNode().getExternalCode(),
                        edge.getLengthMeters(),
                        edge.getMovementType(),
                        edge.isClosed(),
                        edge.isWheelchairAccessible(),
                        edge.getPoints().stream().map(JpaGraphSnapshotLoader::toPoint).toList()))
            .toList();
    var places =
        catalogNavigationReadPort.findActiveRoutablePlaces(mallId).stream()
            .map(JpaGraphSnapshotLoader::toAnchor)
            .toList();
    var kiosks =
        kioskNavigationReadPort.findEnabledKioskStarts(mallId).stream()
            .map(JpaGraphSnapshotLoader::toAnchor)
            .toList();
    return new GraphSnapshot(
        mallId, versionId, version.getVersionCode(), nodes, edges, places, kiosks);
  }

  private static GraphPoint toPoint(NavigationEdgePoint point) {
    return new GraphPoint(point.getX(), point.getY());
  }

  private static GraphAnchor toAnchor(RoutablePlaceReadModel place) {
    return new GraphAnchor(place.placeCode(), place.navigationNodeCode());
  }

  private static GraphAnchor toAnchor(KioskStartReadModel kiosk) {
    return new GraphAnchor(kiosk.kioskCode(), kiosk.navigationNodeCode());
  }
}
