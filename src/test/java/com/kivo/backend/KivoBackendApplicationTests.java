package com.kivo.backend;

import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class KivoBackendApplicationTests extends PostgreSqlIntegrationTest {

  @Test
  void contextLoads() {}
}
