package com.kivo.backend.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.kivo.backend.catalog.application.port.MallIdentityReadPort;
import com.kivo.backend.common.exception.service.MallNotFoundException;
import com.kivo.backend.navigation.application.GraphPublicationService;
import com.kivo.backend.navigation.application.GraphVersionNotFoundException;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminGraphPublicationServiceTest {

  private static final UUID MALL_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID VERSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

  @Mock private MallIdentityReadPort mallIdentityReadPort;
  @Mock private GraphPublicationService graphPublicationService;

  @Test
  void resolvesMallThroughReadPortAndPublishesThroughNavigationService() {
    when(mallIdentityReadPort.findActiveMallIdByCode("demo-mall")).thenReturn(Optional.of(MALL_ID));
    when(graphPublicationService.publish(MALL_ID, VERSION_ID))
        .thenReturn(
            new GraphSnapshot(
                MALL_ID, VERSION_ID, "v1", List.of(), List.of(), List.of(), List.of()));
    var service = new AdminGraphPublicationService(mallIdentityReadPort, graphPublicationService);

    PublishedGraph result = service.publish("demo-mall", VERSION_ID);

    assertThat(result.mallCode()).isEqualTo("demo-mall");
    assertThat(result.versionId()).isEqualTo(VERSION_ID);
    assertThat(result.versionCode()).isEqualTo("v1");
  }

  @Test
  void mapsMissingMallAndGraphVersionToStableServiceErrors() {
    var service = new AdminGraphPublicationService(mallIdentityReadPort, graphPublicationService);
    when(mallIdentityReadPort.findActiveMallIdByCode("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.publish("missing", VERSION_ID))
        .isInstanceOf(MallNotFoundException.class);

    when(mallIdentityReadPort.findActiveMallIdByCode("demo-mall")).thenReturn(Optional.of(MALL_ID));
    when(graphPublicationService.publish(MALL_ID, VERSION_ID))
        .thenThrow(new GraphVersionNotFoundException(MALL_ID, VERSION_ID));

    assertThatThrownBy(() -> service.publish("demo-mall", VERSION_ID))
        .isInstanceOf(AdminGraphVersionNotFoundException.class)
        .extracting(
            exception -> ((AdminGraphVersionNotFoundException) exception).getExceptionCode())
        .isEqualTo("NAVIGATION_GRAPH_VERSION_NOT_FOUND");
  }
}
