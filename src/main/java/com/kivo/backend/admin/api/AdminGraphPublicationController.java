package com.kivo.backend.admin.api;

import com.kivo.backend.admin.application.AdminGraphPublicationService;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/malls/{mallCode}/navigation/graphs")
public class AdminGraphPublicationController {

  private final AdminGraphPublicationService publicationService;

  public AdminGraphPublicationController(AdminGraphPublicationService publicationService) {
    this.publicationService = publicationService;
  }

  @PostMapping("/{versionId}/publish")
  public ResponseEntity<GraphPublicationResponse> publish(
      @PathVariable @Size(min = 1, max = 64) String mallCode, @PathVariable UUID versionId) {
    var published = publicationService.publish(mallCode, versionId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            new GraphPublicationResponse(
                published.mallCode(),
                published.versionId(),
                published.versionCode(),
                published.nodeCount(),
                published.edgeCount()));
  }
}
