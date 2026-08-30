package com.kivo.backend.admin.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.admin.application.AdminGraphPublicationService;
import com.kivo.backend.admin.application.PublishedGraph;
import com.kivo.backend.admin.configuration.ApiAccessDeniedHandler;
import com.kivo.backend.admin.configuration.ApiAuthenticationEntryPoint;
import com.kivo.backend.admin.configuration.SecurityConfiguration;
import com.kivo.backend.common.configuration.WebMvcConfiguration;
import com.kivo.backend.common.web.CorrelationIdFilter;
import com.kivo.backend.common.web.ProblemResponseWriter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(AdminGraphPublicationController.class)
@Import({
  SecurityConfiguration.class,
  ApiAuthenticationEntryPoint.class,
  ApiAccessDeniedHandler.class,
  ProblemResponseWriter.class,
  CorrelationIdFilter.class,
  WebMvcConfiguration.class,
  SecurityMvcTest.PublicProbeController.class
})
@TestPropertySource(
    properties = {
      "kivo.security.admin.username=test-admin",
      "kivo.security.admin.password-hash=$2a$10$jSFJWQ5.f1dUERLQRaGrAeV4lUa.Ki18LHtMLIrF2CQ/9F7nppvim",
      "kivo.security.require-https=false",
      "kivo.security.hsts.max-age=365d",
      "kivo.security.hsts.include-subdomains=true",
      "kivo.security.hsts.preload=true",
      "kivo.http.cors.allowed-origins=https://kiosk.example",
      "kivo.http.cors.allowed-methods=GET,POST,OPTIONS",
      "kivo.http.cors.allowed-headers=Authorization,Content-Type,X-Correlation-ID",
      "kivo.http.cors.exposed-headers=Retry-After,X-Correlation-ID",
      "kivo.http.cors.allow-credentials=false"
    })
class SecurityMvcTest {

  private static final UUID VERSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final String ADMIN_PATH =
      "/api/v1/admin/malls/demo-mall/navigation/graphs/" + VERSION_ID + "/publish";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AdminGraphPublicationService publicationService;

  @Test
  void permitsPublicApiButRequiresStaffBasicAuthenticationForAdmin() throws Exception {
    mockMvc.perform(get("/api/v1/public/ping")).andExpect(status().isOk());

    mockMvc
        .perform(post(ADMIN_PATH).header("X-Correlation-ID", "security-test-123"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Basic realm=\"kivo-admin\""))
        .andExpect(header().string("X-Correlation-ID", "security-test-123"))
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        .andExpect(jsonPath("$.correlationId").value("security-test-123"));

    mockMvc
        .perform(post(ADMIN_PATH).with(user("viewer").roles("USER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void acceptsBcryptBackedStaffCredentialsWithoutCreatingSession() throws Exception {
    when(publicationService.publish("demo-mall", VERSION_ID))
        .thenReturn(new PublishedGraph("demo-mall", VERSION_ID, "v1", 12, 18));

    mockMvc
        .perform(post(ADMIN_PATH).with(httpBasic("test-admin", "password")))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Set-Cookie"))
        .andExpect(jsonPath("$.versionId").value(VERSION_ID.toString()))
        .andExpect(jsonPath("$.nodeCount").value(12));
  }

  @Test
  void appliesExplicitCorsAllowlist() throws Exception {
    mockMvc
        .perform(
            options(ADMIN_PATH)
                .header("Origin", "https://kiosk.example")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "https://kiosk.example"));

    mockMvc
        .perform(
            options(ADMIN_PATH)
                .header("Origin", "https://attacker.example")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }

  @RestController
  static class PublicProbeController {

    @GetMapping("/api/v1/public/ping")
    String ping() {
      return "ok";
    }
  }
}
