package dk.ufst.opendebt.debtservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.common.audit.AuditContextService;
import dk.ufst.opendebt.debtservice.dto.AddLiabilityRequest;
import dk.ufst.opendebt.debtservice.dto.LiabilityDto;
import dk.ufst.opendebt.debtservice.entity.LiabilityEntity.LiabilityType;
import dk.ufst.opendebt.debtservice.service.LiabilityService;

@WebMvcTest(
    controllers = LiabilityController.class,
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration"
    })
class LiabilityControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private LiabilityService liabilityService;
  @MockBean private AuditContextService auditContextService;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final UUID DEBTOR_PERSON_ID = UUID.randomUUID();
  private static final UUID LIABILITY_ID = UUID.randomUUID();

  @Test
  void addLiability_returns201() throws Exception {
    LiabilityDto dto =
        LiabilityDto.builder()
            .id(LIABILITY_ID)
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_PERSON_ID)
            .liabilityType("SOLE")
            .active(true)
            .build();

    when(liabilityService.addLiability(
            eq(DEBT_ID), eq(DEBTOR_PERSON_ID), eq(LiabilityType.SOLE), any()))
        .thenReturn(dto);

    AddLiabilityRequest request = new AddLiabilityRequest();
    request.setDebtorPersonId(DEBTOR_PERSON_ID);
    request.setLiabilityType(LiabilityType.SOLE);

    mockMvc
        .perform(
            post("/api/v1/debts/{debtId}/liabilities", DEBT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.liabilityType").value("SOLE"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void addLiability_proportional_returns201() throws Exception {
    LiabilityDto dto =
        LiabilityDto.builder()
            .id(LIABILITY_ID)
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_PERSON_ID)
            .liabilityType("PROPORTIONAL")
            .sharePercentage(new BigDecimal("60.00"))
            .active(true)
            .build();

    when(liabilityService.addLiability(
            eq(DEBT_ID), eq(DEBTOR_PERSON_ID), eq(LiabilityType.PROPORTIONAL), any()))
        .thenReturn(dto);

    AddLiabilityRequest request = new AddLiabilityRequest();
    request.setDebtorPersonId(DEBTOR_PERSON_ID);
    request.setLiabilityType(LiabilityType.PROPORTIONAL);
    request.setSharePercentage(new BigDecimal("60"));

    mockMvc
        .perform(
            post("/api/v1/debts/{debtId}/liabilities", DEBT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.liabilityType").value("PROPORTIONAL"))
        .andExpect(jsonPath("$.sharePercentage").value(60.00));
  }

  @Test
  void removeLiability_returns204() throws Exception {
    doNothing().when(liabilityService).removeLiability(LIABILITY_ID);

    mockMvc
        .perform(delete("/api/v1/debts/{debtId}/liabilities/{liabilityId}", DEBT_ID, LIABILITY_ID))
        .andExpect(status().isNoContent());
  }

  @Test
  void getLiabilities_returnsOk() throws Exception {
    LiabilityDto dto =
        LiabilityDto.builder()
            .id(LIABILITY_ID)
            .debtId(DEBT_ID)
            .debtorPersonId(DEBTOR_PERSON_ID)
            .liabilityType("JOINT_AND_SEVERAL")
            .active(true)
            .build();

    when(liabilityService.getLiabilities(DEBT_ID)).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/v1/debts/{debtId}/liabilities", DEBT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].liabilityType").value("JOINT_AND_SEVERAL"))
        .andExpect(jsonPath("$[0].active").value(true));
  }

  @Test
  void getLiabilities_emptyList_returnsOk() throws Exception {
    when(liabilityService.getLiabilities(DEBT_ID)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/debts/{debtId}/liabilities", DEBT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }
}
