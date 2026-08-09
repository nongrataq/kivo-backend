package com.kivo.backend.catalog.api;

import com.kivo.backend.catalog.application.PlaceQueryService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/malls/{mallCode}/places")
public class PlaceController {

  private final PlaceQueryService placeQueryService;

  public PlaceController(PlaceQueryService placeQueryService) {
    this.placeQueryService = placeQueryService;
  }

  @GetMapping
  public List<PlaceResponse> getPlaces(
      @PathVariable @Size(min = 1, max = 64) String mallCode,
      @RequestParam(required = false) @Size(max = 100) String q,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(defaultValue = "ru") @Pattern(regexp = "[a-z]{2,3}") String lang) {
    return placeQueryService.findPlaces(mallCode, q, categoryId, lang);
  }
}
