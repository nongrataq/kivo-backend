package com.kivo.backend.sharing.persistence.entity;

import com.kivo.backend.common.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "shared_route")
public class SharedRoute extends AbstractUuidEntity {

  @NotNull
  @Column(name = "route_id", nullable = false)
  private UUID routeId;

  @NotNull
  @Column(name = "mall_id", nullable = false)
  private UUID mallId;

  @NotNull
  @Column(name = "graph_version_id", nullable = false)
  private UUID graphVersionId;

  @NotBlank
  @Size(min = 64, max = 64)
  @Pattern(regexp = "[0-9a-f]{64}")
  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @NotBlank
  @Column(name = "snapshot_json", nullable = false, columnDefinition = "text")
  private String snapshotJson;

  @NotNull
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @NotNull
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public static SharedRoute create(
      UUID routeId,
      UUID mallId,
      UUID graphVersionId,
      String tokenHash,
      String snapshotJson,
      Instant createdAt,
      Instant expiresAt) {
    if (!Objects.requireNonNull(expiresAt).isAfter(Objects.requireNonNull(createdAt))) {
      throw new IllegalArgumentException("Shared route expiry must be after creation time");
    }
    SharedRoute route = new SharedRoute();
    route.routeId = Objects.requireNonNull(routeId);
    route.mallId = Objects.requireNonNull(mallId);
    route.graphVersionId = Objects.requireNonNull(graphVersionId);
    route.tokenHash = Objects.requireNonNull(tokenHash);
    route.snapshotJson = Objects.requireNonNull(snapshotJson);
    route.createdAt = createdAt;
    route.expiresAt = expiresAt;
    return route;
  }

  public void revoke(Instant revokedAt) {
    Instant value = Objects.requireNonNull(revokedAt);
    if (value.isBefore(createdAt)) {
      throw new IllegalArgumentException("Revocation time must not precede creation time");
    }
    if (this.revokedAt == null) {
      this.revokedAt = value;
    }
  }

  @Override
  public String toString() {
    return "SharedRoute{"
        + "id="
        + getId()
        + ", routeId="
        + routeId
        + ", mallId="
        + mallId
        + ", graphVersionId="
        + graphVersionId
        + ", createdAt="
        + createdAt
        + ", expiresAt="
        + expiresAt
        + ", revokedAt="
        + revokedAt
        + '}';
  }
}
