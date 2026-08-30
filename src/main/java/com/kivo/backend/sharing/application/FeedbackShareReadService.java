package com.kivo.backend.sharing.application;

import com.kivo.backend.sharing.persistence.repository.SharedRouteRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackShareReadService implements FeedbackShareReadPort {

  private final SharedRouteRepository sharedRouteRepository;
  private final SharedRouteSnapshotCodec snapshotCodec;
  private final Clock clock;

  @Override
  @Transactional(readOnly = true)
  public Optional<FeedbackShareReadModel> findFeedbackShare(UUID shareId) {
    return sharedRouteRepository
        .findByIdAndRevokedAtIsNullAndExpiresAtAfter(shareId, clock.instant())
        .map(
            sharedRoute -> {
              SharedRouteSnapshot snapshot = snapshotCodec.decode(sharedRoute.getSnapshotJson());
              return new FeedbackShareReadModel(
                  sharedRoute.getId(),
                  sharedRoute.getMallId(),
                  snapshot.kioskCode(),
                  snapshot.placeId());
            });
  }
}
