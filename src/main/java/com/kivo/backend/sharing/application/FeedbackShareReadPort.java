package com.kivo.backend.sharing.application;

import java.util.Optional;
import java.util.UUID;

public interface FeedbackShareReadPort {

  Optional<FeedbackShareReadModel> findFeedbackShare(UUID shareId);
}
