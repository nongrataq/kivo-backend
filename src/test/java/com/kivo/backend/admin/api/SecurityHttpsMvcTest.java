package com.kivo.backend.admin.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.admin.application.AdminGraphPublicationService;
import com.kivo.backend.admin.application.PublishedGraph;
import com.kivo.backend.admin.configuration.ApiAccessDeniedHandler;
import com.kivo.backend.admin.configuration.ApiAuthenticationEntryPoint;
import com.kivo.backend.admin.configuration.SecurityConfiguration;
import com.kivo.backend.common.configuration.WebMvcConfiguration;
import com.kivo.backend.common.web.CorrelationIdFilter;
import com.kivo.backend.common.web.ProblemResponseWriter;
import java.net.URI;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminGraphPublicationController.class)
@Import({
  SecurityConfiguration.class,
  ApiAuthenticationEntryPoint.class,
  ApiAccessDeniedHandler.class,
  ProblemResponseWriter.class,
  CorrelationIdFilter.class,
  WebMvcConfiguration.class
})
@TestPropertySource(
    properties = {
      "kivo.security.admin.username=test-admin",
      "kivo.security.admin.password-hash=$2a$10$jSFJWQ5.f1dUERLQRaGrAeV4lUa.Ki18LHtMLIrF2CQ/9F7nppvim",
      "kivo.security.require-https=true",
      "kivo.security.hsts.max-age=365d",
      "kivo.security.hsts.include-subdomains=true",
      "kivo.security.hsts.preload=true",
      "kivo.http.cors.allowed-origins=",
      "kivo.http.cors.allowed-methods=GET,POST,OPTIONS",
      "kivo.http.cors.allowed-headers=Authorization,Content-Type",
      "kivo.http.cors.exposed-headers=X-Correlation-ID",
      "kivo.http.cors.allow-credentials=false"
    })
class SecurityHttpsMvcTest {

  private static final UUID VERSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final String ADMIN_PATH =
      "/api/v1/admin/malls/demo-mall/navigation/graphs/" + VERSION_ID + "/publish";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AdminGraphPublicationService publicationService;

  @Test
  void redirectsPlainHttpAndWritesHstsOnlyOnSecureResponses() throws Exception {
    when(publicationService.publish("demo-mall", VERSION_ID))
        .thenReturn(new PublishedGraph("demo-mall", VERSION_ID, "v1", 12, 18));

    mockMvc
        .perform(post(ADMIN_PATH).secure(false))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", Matchers.startsWith("https://")))
        .andExpect(header().doesNotExist("Strict-Transport-Security"));

    mockMvc
        .perform(
            post(URI.create("https://localhost" + ADMIN_PATH))
                .secure(true)
                .with(httpBasic("test-admin", "password")))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Strict-Transport-Security",
                    Matchers.allOf(
                        Matchers.containsString("max-age=31536000"),
                        Matchers.containsString("includeSubDomains"),
                        Matchers.containsString("preload"))));
  }
}
