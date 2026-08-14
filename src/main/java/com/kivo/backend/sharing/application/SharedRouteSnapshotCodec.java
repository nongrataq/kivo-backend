package com.kivo.backend.sharing.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class SharedRouteSnapshotCodec {

  private final ObjectMapper objectMapper;

  public String encode(SharedRouteSnapshot snapshot) {
    try {
      return objectMapper.writeValueAsString(snapshot);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Failed to encode shared route snapshot", exception);
    }
  }

  public SharedRouteSnapshot decode(String json) {
    try {
      SharedRouteSnapshot snapshot = objectMapper.readValue(json, SharedRouteSnapshot.class);
      if (snapshot.schemaVersion() != SharedRouteSnapshot.CURRENT_SCHEMA_VERSION) {
        throw new IllegalStateException(
            "Unsupported shared route snapshot version: " + snapshot.schemaVersion());
      }
      return snapshot;
    } catch (JacksonException exception) {
      throw new IllegalStateException("Failed to decode shared route snapshot", exception);
    }
  }
}
