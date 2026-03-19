package dk.ufst.opendebt.debtservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

import dk.ufst.opendebt.debtservice.dto.ObjectionDto;
import dk.ufst.opendebt.debtservice.dto.RegisterObjectionRequest;
import dk.ufst.opendebt.debtservice.dto.ResolveObjectionRequest;
import dk.ufst.opendebt.debtservice.entity.ObjectionEntity.ObjectionStatus;
import dk.ufst.opendebt.debtservice.service.ObjectionService;

@WebMvcTest(
    controllers = ObjectionController.class,
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration"
    })
class ObjectionControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private ObjectionService objectionService;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final UUID DEBTOR_PERSON_ID = UUID.randomUUID();
  private static final UUID OBJECTION_ID = UUID.randomUUID();

  @Test
  void registerObjection_returns201() throws Exception {
    ObjectionDto dto =
        ObjectionDto.builder()
            .id(OBJECTION_ID)
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_PERSON_ID)
            .reason("claim amount disputed")
            .status("ACTIVE")
            .registeredAt(Instant.now())
            .build();

    when(objectionService.registerObjection(eq(DEBT_ID), eq(DEBTOR_PERSON_ID), any()))
        .thenReturn(dto);

    RegisterObjectionRequest request = new RegisterObjectionRequest();
    request.setDebtorPersonId(DEBTOR_PERSON_ID);
    request.setReason("claim amount disputed");

    mockMvc
        .perform(
            post("/api/v1/debts/{debtId}/objections", DEBT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.reason").value("claim amount disputed"));
  }

  @Test
  void resolveObjection_returnsOk() throws Exception {
    ObjectionDto dto =
        ObjectionDto.builder()
            .id(OBJECTION_ID)
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_PERSON_ID)
            .reason("claim amount disputed")
            .status("REJECTED")
            .resolvedAt(Instant.now())
            .resolutionNote("insufficient evidence")
            .build();

    when(objectionService.resolveObjection(eq(OBJECTION_ID), eq(ObjectionStatus.REJECTED), any()))
        .thenReturn(dto);

    ResolveObjectionRequest request = new ResolveObjectionRequest();
    request.setOutcome(ObjectionStatus.REJECTED);
    request.setNote("insufficient evidence");

    mockMvc
        .perform(
            put("/api/v1/debts/{debtId}/objections/{objectionId}/resolve", DEBT_ID, OBJECTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.resolutionNote").value("insufficient evidence"));
  }

  @Test
  void getObjections_returnsOk() throws Exception {
    ObjectionDto dto =
        ObjectionDto.builder()
            .id(OBJECTION_ID)
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_PERSON_ID)
            .reason("disputed")
            .status("ACTIVE")
            .registeredAt(Instant.now())
            .build();

    when(objectionService.getObjections(eq(DEBT_ID))).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/v1/debts/{debtId}/objections", DEBT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));
  }

  @Test
  void getObjections_emptyList_returnsOk() throws Exception {
    when(objectionService.getObjections(eq(DEBT_ID))).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/debts/{debtId}/objections", DEBT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }
}
