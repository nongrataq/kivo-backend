package com.kivo.backend.sharing.application;

import static com.kivo.backend.sharing.SharingTestFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SharedRouteSnapshotCodecTest {

  private final SharedRouteSnapshotCodec codec =
      new SharedRouteSnapshotCodec(JsonMapper.builder().findAndAddModules().build());

  @Test
  void roundTripsTheCompleteVersionedSnapshot() {
    SharedRouteSnapshot expected = snapshot();

    String json = codec.encode(expected);

    assertThat(codec.decode(json)).isEqualTo(expected);
    assertThat(json).contains("\"schemaVersion\":1", "\"mapImageUrl\"");
  }

  @Test
  void rejectsUnknownSnapshotVersions() {
    String json = codec.encode(snapshot()).replace("\"schemaVersion\":1", "\"schemaVersion\":2");

    assertThatThrownBy(() -> codec.decode(json))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Unsupported shared route snapshot version: 2");
  }
}
