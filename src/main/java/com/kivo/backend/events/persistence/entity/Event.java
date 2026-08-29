package com.kivo.backend.events.persistence.entity;

import com.kivo.backend.common.persistence.AbstractUuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "event")
public class Event extends AbstractUuidEntity {

  @NotNull
  @Column(name = "mall_id", nullable = false)
  private UUID mallId;

  @NotNull
  @Column(name = "place_id", nullable = false)
  private UUID placeId;

  @NotBlank
  @Size(max = 64)
  @Pattern(regexp = "[a-z0-9][a-z0-9-]{0,63}")
  @Column(name = "code", nullable = false, length = 64)
  private String code;

  @NotNull
  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @NotNull
  @Column(name = "ends_at", nullable = false)
  private Instant endsAt;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "publication_state", nullable = false, length = 16)
  private EventPublicationState publicationState;

  @NotNull
  @Column(name = "publication_starts_at", nullable = false)
  private Instant publicationStartsAt;

  @Column(name = "publication_ends_at")
  private Instant publicationEndsAt;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @OneToMany(
      mappedBy = "event",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private Set<EventTranslation> translations = new LinkedHashSet<>();

  public static Event create(
      UUID mallId,
      UUID placeId,
      String code,
      Instant startsAt,
      Instant endsAt,
      EventPublicationState publicationState,
      Instant publicationStartsAt,
      Instant publicationEndsAt,
      int displayOrder) {
    if (!Objects.requireNonNull(endsAt).isAfter(Objects.requireNonNull(startsAt))) {
      throw new IllegalArgumentException("Event end must be after start");
    }
    if (publicationEndsAt != null
        && !publicationEndsAt.isAfter(Objects.requireNonNull(publicationStartsAt))) {
      throw new IllegalArgumentException("Publication end must be after start");
    }
    Event event = new Event();
    event.mallId = Objects.requireNonNull(mallId);
    event.placeId = Objects.requireNonNull(placeId);
    event.code = Objects.requireNonNull(code);
    event.startsAt = startsAt;
    event.endsAt = endsAt;
    event.publicationState = Objects.requireNonNull(publicationState);
    event.publicationStartsAt = publicationStartsAt;
    event.publicationEndsAt = publicationEndsAt;
    event.displayOrder = displayOrder;
    return event;
  }

  public Set<EventTranslation> getTranslations() {
    return Collections.unmodifiableSet(translations);
  }

  public void addTranslation(EventTranslation translation) {
    translations.add(translation);
    translation.attachTo(this);
  }

  public void removeTranslation(EventTranslation translation) {
    translations.remove(translation);
    translation.detach();
  }

  @Override
  public String toString() {
    return "Event{"
        + "id="
        + getId()
        + ", mallId="
        + mallId
        + ", placeId="
        + placeId
        + ", code='"
        + code
        + '\''
        + ", startsAt="
        + startsAt
        + ", endsAt="
        + endsAt
        + ", publicationState="
        + publicationState
        + '}';
  }
}
