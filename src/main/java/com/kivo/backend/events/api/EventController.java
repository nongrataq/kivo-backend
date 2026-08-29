package com.kivo.backend.events.api;

import com.kivo.backend.events.application.EventQueryService;
import com.kivo.backend.events.application.EventResponse;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
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
@RequestMapping("/api/v1/malls/{mallCode}/events")
public class EventController {

  private final EventQueryService eventQueryService;

  @GetMapping("/today")
  public List<EventResponse> getToday(
      @PathVariable @Size(min = 1, max = 64, message = "{validation.mallCode.size}")
          String mallCode,
      @RequestParam(required = false)
          @Pattern(regexp = "[a-z]{2,3}", message = "{validation.language.pattern}")
          String lang) {
    return eventQueryService.findToday(mallCode, lang);
  }
}
