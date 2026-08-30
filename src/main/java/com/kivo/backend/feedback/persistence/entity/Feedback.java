package com.kivo.backend.feedback.persistence.entity;

import com.kivo.backend.common.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "feedback")
public class Feedback extends AbstractUuidEntity {

  @NotNull
  @Column(name = "mall_id", nullable = false)
  private UUID mallId;

  @NotNull
  @Column(name = "kiosk_id", nullable = false)
  private UUID kioskId;

  @NotNull
  @Column(name = "place_id", nullable = false)
  private UUID placeId;

  @Column(name = "route_id")
  private UUID routeId;

  @Column(name = "share_id")
  private UUID shareId;

  @Min(1)
  @Max(5)
  @Column(name = "rating", nullable = false)
  private int rating;

  @NotNull
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public static Feedback forRoute(
      UUID mallId, UUID kioskId, UUID placeId, UUID routeId, int rating, Instant createdAt) {
    return create(mallId, kioskId, placeId, routeId, null, rating, createdAt);
  }

  public static Feedback forShare(
      UUID mallId, UUID kioskId, UUID placeId, UUID shareId, int rating, Instant createdAt) {
    return create(mallId, kioskId, placeId, null, shareId, rating, createdAt);
  }

  private static Feedback create(
      UUID mallId,
      UUID kioskId,
      UUID placeId,
      UUID routeId,
      UUID shareId,
      int rating,
      Instant createdAt) {
    if ((routeId == null) == (shareId == null)) {
      throw new IllegalArgumentException("Exactly one feedback context is required");
    }
    if (rating < 1 || rating > 5) {
      throw new IllegalArgumentException("Feedback rating must be between 1 and 5");
    }
    Feedback feedback = new Feedback();
    feedback.mallId = Objects.requireNonNull(mallId);
    feedback.kioskId = Objects.requireNonNull(kioskId);
    feedback.placeId = Objects.requireNonNull(placeId);
    feedback.routeId = routeId;
    feedback.shareId = shareId;
    feedback.rating = rating;
    feedback.createdAt = Objects.requireNonNull(createdAt);
    return feedback;
  }

  @Override
  public String toString() {
    return "Feedback{"
        + "id="
        + getId()
        + ", mallId="
        + mallId
        + ", kioskId="
        + kioskId
        + ", placeId="
        + placeId
        + ", routeId="
        + routeId
        + ", shareId="
        + shareId
        + ", rating="
        + rating
        + ", createdAt="
        + createdAt
        + '}';
  }
}
