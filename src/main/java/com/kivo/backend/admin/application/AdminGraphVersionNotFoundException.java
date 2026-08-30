package com.kivo.backend.admin.application;

import static com.kivo.backend.common.exception.message.ExceptionKeys.ADMIN_GRAPH_VERSION_NOT_FOUND;

import com.kivo.backend.common.exception.service.ServiceException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public final class AdminGraphVersionNotFoundException extends ServiceException {

  public AdminGraphVersionNotFoundException(String mallCode, UUID versionId, Throwable cause) {
    super(
        HttpStatus.NOT_FOUND,
        "NAVIGATION_GRAPH_VERSION_NOT_FOUND",
        ADMIN_GRAPH_VERSION_NOT_FOUND,
        versionId,
        mallCode);
    initCause(cause);
  }
}
