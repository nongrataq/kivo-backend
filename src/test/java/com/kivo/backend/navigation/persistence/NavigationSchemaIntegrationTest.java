package com.kivo.backend.navigation.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kivo.backend.catalog.persistence.repository.FloorRepository;
import com.kivo.backend.catalog.persistence.repository.MallRepository;
import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.model.NodeType;
import com.kivo.backend.navigation.persistence.entity.NavigationEdge;
import com.kivo.backend.navigation.persistence.entity.NavigationEdgePoint;
import com.kivo.backend.navigation.persistence.entity.NavigationGraphVersion;
import com.kivo.backend.navigation.persistence.entity.NavigationNode;
import com.kivo.backend.navigation.persistence.repository.NavigationEdgeRepository;
import com.kivo.backend.navigation.persistence.repository.NavigationGraphVersionRepository;
import com.kivo.backend.navigation.persistence.repository.NavigationNodeRepository;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Sql("/sql/category-test-data.sql")
class NavigationSchemaIntegrationTest extends PostgreSqlIntegrationTest {

  private static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID SECOND_MALL_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000800");
  private static final UUID FLOOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MallRepository mallRepository;
  @Autowired private FloorRepository floorRepository;
  @Autowired private NavigationGraphVersionRepository graphVersionRepository;
  @Autowired private NavigationNodeRepository nodeRepository;
  @Autowired private NavigationEdgeRepository edgeRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void repositoriesPersistAndLoadOrderedGraphGeometry() {
    var mall = mallRepository.findById(MALL_ID).orElseThrow();
    var floor = floorRepository.findById(FLOOR_ID).orElseThrow();
    var version = graphVersionRepository.save(NavigationGraphVersion.create(mall, "repository"));
    var start =
        nodeRepository.save(
            NavigationNode.create(version, floor, "a-start", 100, 100, NodeType.KIOSK));
    var destination =
        nodeRepository.save(
            NavigationNode.create(
                version, floor, "b-destination", 200, 200, NodeType.PLACE_ACCESS));
    var edge =
        NavigationEdge.create(
            version,
            "a-edge",
            start,
            destination,
            BigDecimal.TEN,
            MovementType.CORRIDOR,
            false,
            true);
    edge.addPoint(NavigationEdgePoint.create(1, 200, 200));
    edge.addPoint(NavigationEdgePoint.create(0, 100, 100));
    edgeRepository.saveAndFlush(edge);
    entityManager.clear();

    assertThat(nodeRepository.findAllByGraphVersionIdOrderByExternalCode(version.getId()))
        .extracting(NavigationNode::getExternalCode)
        .containsExactly("a-start", "b-destination");
    assertThat(edgeRepository.findAllWithDetailsByGraphVersionId(version.getId()))
        .singleElement()
        .satisfies(
            loaded ->
                assertThat(loaded.getPoints())
                    .extracting(NavigationEdgePoint::getPointOrder)
                    .containsExactly(0, 1));
  }

  @Test
  void graphVersionCodeIsUniqueWithinMall() {
    insertGraphVersion(versionId(1), MALL_ID, "v1");

    assertThatThrownBy(() -> insertGraphVersion(versionId(2), MALL_ID, "v1"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsNodeOutsideNormalizedCoordinateRange() {
    UUID versionId = versionId(3);
    insertGraphVersion(versionId, MALL_ID, "coordinates");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    insert into navigation_node
                      (id, graph_version_id, floor_id, external_code, x, y, node_type)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """,
                    versionId(30),
                    versionId,
                    FLOOR_ID,
                    "outside",
                    10001,
                    100,
                    "WAYPOINT"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsNonPositiveEdgeLength() {
    UUID versionId = versionId(4);
    insertGraphVersion(versionId, MALL_ID, "edge-length");
    insertNode(versionId(40), versionId, "from", 100, 100);
    insertNode(versionId(41), versionId, "to", 200, 200);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    insert into navigation_edge
                      (id, graph_version_id, external_code, from_node_id, to_node_id,
                       length_meters, movement_type, closed, wheelchair_accessible)
                    values (?, ?, ?, ?, ?, ?, ?, false, true)
                    """,
                    versionId(42),
                    versionId,
                    "invalid-edge",
                    versionId(40),
                    versionId(41),
                    0,
                    "CORRIDOR"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void publicationCannotReferenceVersionFromAnotherMall() {
    UUID versionId = versionId(5);
    insertGraphVersion(versionId, MALL_ID, "published");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    insert into navigation_graph_publication
                      (id, mall_id, active_graph_version_id, published_at)
                    values (?, ?, ?, current_timestamp)
                    """,
                    versionId(50),
                    SECOND_MALL_ID,
                    versionId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void edgePointOrderIsUniquePerEdge() {
    UUID versionId = versionId(6);
    UUID edgeId = versionId(62);
    insertGraphVersion(versionId, MALL_ID, "geometry");
    insertNode(versionId(60), versionId, "from", 100, 100);
    insertNode(versionId(61), versionId, "to", 200, 200);
    jdbcTemplate.update(
        """
        insert into navigation_edge
          (id, graph_version_id, external_code, from_node_id, to_node_id,
           length_meters, movement_type, closed, wheelchair_accessible)
        values (?, ?, ?, ?, ?, ?, ?, false, true)
        """,
        edgeId,
        versionId,
        "edge",
        versionId(60),
        versionId(61),
        10,
        "CORRIDOR");
    insertPoint(versionId(63), edgeId, 0);

    assertThatThrownBy(() -> insertPoint(versionId(64), edgeId, 0))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private void insertGraphVersion(UUID id, UUID mallId, String code) {
    jdbcTemplate.update(
        """
        insert into navigation_graph_version
          (id, mall_id, version_code, state, created_at)
        values (?, ?, ?, 'DRAFT', current_timestamp)
        """,
        id,
        mallId,
        code);
  }

  private void insertNode(UUID id, UUID versionId, String code, int x, int y) {
    jdbcTemplate.update(
        """
        insert into navigation_node
          (id, graph_version_id, floor_id, external_code, x, y, node_type)
        values (?, ?, ?, ?, ?, ?, 'WAYPOINT')
        """,
        id,
        versionId,
        FLOOR_ID,
        code,
        x,
        y);
  }

  private void insertPoint(UUID id, UUID edgeId, int pointOrder) {
    jdbcTemplate.update(
        """
        insert into navigation_edge_point (id, edge_id, point_order, x, y)
        values (?, ?, ?, ?, ?)
        """,
        id,
        edgeId,
        pointOrder,
        100,
        100);
  }

  private static UUID versionId(long suffix) {
    return UUID.fromString("10000000-0000-0000-0000-%012d".formatted(suffix));
  }
}
