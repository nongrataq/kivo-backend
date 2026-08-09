package com.kivo.backend.catalog.application.port;

import java.util.List;

public record CatalogBootstrapReadModel(
    String mallCode,
    String mallName,
    String defaultLanguage,
    String selectedLanguage,
    List<CatalogLanguageReadModel> languages,
    List<CatalogFloorReadModel> floors) {}
