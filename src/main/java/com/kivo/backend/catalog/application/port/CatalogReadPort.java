package com.kivo.backend.catalog.application.port;

import java.util.Optional;
import java.util.UUID;

public interface CatalogReadPort {

  Optional<CatalogBootstrapReadModel> findActiveCatalog(UUID mallId, String requestedLanguage);
}
