package com.kivo.backend.operations;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMetrics
@ActiveProfiles("test")
class SecurityOperationsIntegrationTest extends PostgreSqlIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void enforcesAdminPermissionInFullApplicationContext() throws Exception {
    String adminPath =
        "/api/v1/admin/malls/missing/navigation/graphs/" + UUID.randomUUID() + "/publish";

    mockMvc.perform(post(adminPath)).andExpect(status().isUnauthorized());
    mockMvc
        .perform(post(adminPath).with(httpBasic("test-admin", "password")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MALL_NOT_FOUND"));
    mockMvc
        .perform(get("/api/v1/malls/missing/places"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MALL_NOT_FOUND"));
  }

  @Test
  void exposesPrometheusAndSeparatesLivenessFromDegradedReadiness() throws Exception {
    mockMvc
        .perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mockMvc
        .perform(get("/actuator/health/readiness"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
    mockMvc
        .perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("# HELP")))
        .andExpect(header().exists("X-Correlation-ID"));
  }
}
