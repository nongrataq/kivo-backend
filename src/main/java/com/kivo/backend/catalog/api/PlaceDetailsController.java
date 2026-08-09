package com.kivo.backend.catalog.api;

import com.kivo.backend.catalog.application.PlaceQueryService;
import com.kivo.backend.catalog.application.query.PlaceResponse;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/places")
public class PlaceDetailsController {

  private final PlaceQueryService placeQueryService;

  @GetMapping("/{placeId}")
  public PlaceResponse getPlace(
      @PathVariable UUID placeId,
      @RequestParam(required = false)
          @Pattern(regexp = "[a-z]{2,3}", message = "{validation.language.pattern}")
          String lang) {
    return placeQueryService.findPlace(placeId, lang);
  }
}
