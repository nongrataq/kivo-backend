package com.kivo.backend.kiosks.api;

import com.kivo.backend.kiosks.application.KioskBootstrapService;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/api/v1/kiosks")
public class KioskController {

  private final KioskBootstrapService kioskBootstrapService;

  @GetMapping("/{kioskCode}/bootstrap")
  public KioskBootstrapResponse getBootstrap(
      @PathVariable @Size(min = 1, max = 64, message = "{validation.kioskCode.size}")
          String kioskCode,
      @RequestParam(required = false)
          @Pattern(regexp = "[a-z]{2,3}", message = "{validation.language.pattern}")
          String lang) {
    return kioskBootstrapService.getBootstrap(kioskCode, lang);
  }
}
