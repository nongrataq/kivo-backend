package com.kivo.backend.kiosks.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.common.configuration.WebMvcConfiguration;
import com.kivo.backend.common.exception.handler.GlobalExceptionHandler;
import com.kivo.backend.common.exception.service.KioskNotFoundException;
import com.kivo.backend.kiosks.application.KioskBootstrapService;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse.FloorResponse;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse.LanguageResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KioskController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfiguration.class})
class KioskControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private KioskBootstrapService kioskBootstrapService;

  @Test
  void returnsBootstrapAndPassesOptionalLanguage() throws Exception {
    when(kioskBootstrapService.getBootstrap("demo-kiosk", "en"))
        .thenReturn(
            new KioskBootstrapResponse(
                "demo-kiosk",
                "demo-mall",
                "Демо ТЦ",
                "ru",
                "en",
                "L1",
                List.of(new LanguageResponse("ru", "RU", 10)),
                List.of(
                    new FloorResponse(
                        "L1",
                        "Первый этаж",
                        1,
                        10,
                        "https://cdn.example.com/maps/demo-mall/L1.png",
                        10000,
                        10000))));

    mockMvc
        .perform(get("/api/v1/kiosks/demo-kiosk/bootstrap").param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.kioskCode").value("demo-kiosk"))
        .andExpect(jsonPath("$.selectedLanguage").value("en"))
        .andExpect(jsonPath("$.languages[0].code").value("ru"))
        .andExpect(jsonPath("$.floors[0].code").value("L1"))
        .andExpect(jsonPath("$.floors[0].mapImageUrl").isString())
        .andExpect(jsonPath("$.startNavigationNodeCode").doesNotExist());

    verify(kioskBootstrapService).getBootstrap("demo-kiosk", "en");
  }

  @Test
  void passesNullWhenLanguageIsOmitted() throws Exception {
    when(kioskBootstrapService.getBootstrap("demo-kiosk", null))
        .thenReturn(
            new KioskBootstrapResponse(
                "demo-kiosk", "demo-mall", "Демо ТЦ", "ru", "ru", "L1", List.of(), List.of()));

    mockMvc.perform(get("/api/v1/kiosks/demo-kiosk/bootstrap")).andExpect(status().isOk());

    verify(kioskBootstrapService).getBootstrap("demo-kiosk", null);
  }

  @Test
  void returnsLocalizedProblemForUnknownKiosk() throws Exception {
    when(kioskBootstrapService.getBootstrap("unknown", null))
        .thenThrow(new KioskNotFoundException("unknown"));

    mockMvc
        .perform(get("/api/v1/kiosks/unknown/bootstrap"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("KIOSK_NOT_FOUND"))
        .andExpect(jsonPath("$.detail").value("Киоск с кодом unknown не найден"));
  }

  @Test
  void rejectsMalformedLanguage() throws Exception {
    mockMvc
        .perform(get("/api/v1/kiosks/demo-kiosk/bootstrap").param("lang", "en-US"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
