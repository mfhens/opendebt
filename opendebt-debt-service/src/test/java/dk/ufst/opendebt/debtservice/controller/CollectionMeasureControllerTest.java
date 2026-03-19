package dk.ufst.opendebt.debtservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.debtservice.dto.CollectionMeasureDto;
import dk.ufst.opendebt.debtservice.dto.InitiateMeasureRequest;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity.MeasureType;
import dk.ufst.opendebt.debtservice.service.CollectionMeasureService;

@WebMvcTest(
    controllers = CollectionMeasureController.class,
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration"
    })
class CollectionMeasureControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private CollectionMeasureService measureService;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final UUID MEASURE_ID = UUID.randomUUID();

  @Test
  void initiateMeasure_returns201() throws Exception {
    CollectionMeasureDto dto =
        CollectionMeasureDto.builder()
            .id(MEASURE_ID)
            .debtId(DEBT_ID)
            .measureType("SET_OFF")
            .status("INITIATED")
            .amount(new BigDecimal("5000"))
            .initiatedAt(Instant.now())
            .build();

    when(measureService.initiateMeasure(eq(DEBT_ID), eq(MeasureType.SET_OFF), any(), any()))
        .thenReturn(dto);

    InitiateMeasureRequest request = new InitiateMeasureRequest();
    request.setMeasureType(MeasureType.SET_OFF);
    request.setAmount(new BigDecimal("5000"));

    mockMvc
        .perform(
            post("/api/v1/debts/{debtId}/collection-measures", DEBT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.measureType").value("SET_OFF"))
        .andExpect(jsonPath("$.status").value("INITIATED"));
  }

  @Test
  void completeMeasure_returnsOk() throws Exception {
    CollectionMeasureDto dto =
        CollectionMeasureDto.builder()
            .id(MEASURE_ID)
            .debtId(DEBT_ID)
            .measureType("SET_OFF")
            .status("COMPLETED")
            .completedAt(Instant.now())
            .build();

    when(measureService.completeMeasure(eq(MEASURE_ID))).thenReturn(dto);

    mockMvc
        .perform(
            post(
                "/api/v1/debts/{debtId}/collection-measures/{measureId}/complete",
                DEBT_ID,
                MEASURE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  void cancelMeasure_returnsOk() throws Exception {
    CollectionMeasureDto dto =
        CollectionMeasureDto.builder()
            .id(MEASURE_ID)
            .debtId(DEBT_ID)
            .measureType("WAGE_GARNISHMENT")
            .status("CANCELLED")
            .build();

    when(measureService.cancelMeasure(eq(MEASURE_ID), any())).thenReturn(dto);

    mockMvc
        .perform(
            post(
                "/api/v1/debts/{debtId}/collection-measures/{measureId}/cancel",
                DEBT_ID,
                MEASURE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void getMeasures_returnsOk() throws Exception {
    CollectionMeasureDto dto =
        CollectionMeasureDto.builder()
            .id(MEASURE_ID)
            .debtId(DEBT_ID)
            .measureType("ATTACHMENT")
            .status("INITIATED")
            .initiatedAt(Instant.now())
            .build();

    when(measureService.getMeasures(eq(DEBT_ID))).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/v1/debts/{debtId}/collection-measures", DEBT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].measureType").value("ATTACHMENT"));
  }

  @Test
  void getMeasures_emptyList_returnsOk() throws Exception {
    when(measureService.getMeasures(eq(DEBT_ID))).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/debts/{debtId}/collection-measures", DEBT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }
}
