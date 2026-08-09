package com.kivo.backend.kiosks.application.query;

import java.util.List;

public record KioskBootstrapResponse(
    String kioskCode,
    String mallCode,
    String mallName,
    String defaultLanguage,
    String selectedLanguage,
    String currentFloorCode,
    List<LanguageResponse> languages,
    List<FloorResponse> floors) {

  public record LanguageResponse(String code, String displayLabel, int displayOrder) {}

  public record FloorResponse(
      String code,
      String name,
      int levelNumber,
      int displayOrder,
      String mapImageUrl,
      int coordinateWidth,
      int coordinateHeight) {}
}
