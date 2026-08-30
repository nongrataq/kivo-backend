package com.kivo.backend.events.persistence.repository;

import com.kivo.backend.events.persistence.entity.Event;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, UUID> {

  @Query(
      """
      select event.id
      from Event event
      where event.mallId = :mallId
        and event.placeId in :placeIds
        and event.publicationState = com.kivo.backend.events.persistence.entity.EventPublicationState.PUBLISHED
        and event.startsAt < :dayEnd
        and event.endsAt > :dayStart
        and event.publicationStartsAt <= :now
        and (event.publicationEndsAt is null or event.publicationEndsAt > :now)
      order by event.startsAt, event.displayOrder, event.code, event.id
      """)
  List<UUID> findPublishedTodayIds(
      @Param("mallId") UUID mallId,
      @Param("placeIds") Collection<UUID> placeIds,
      @Param("dayStart") Instant dayStart,
      @Param("dayEnd") Instant dayEnd,
      @Param("now") Instant now,
      Pageable pageable);

  @Query(
      """
      select distinct event
      from Event event
      left join fetch event.translations translation
      where event.id in :ids
      """)
  List<Event> findAllWithTranslationsByIdIn(@Param("ids") Collection<UUID> ids);
}
