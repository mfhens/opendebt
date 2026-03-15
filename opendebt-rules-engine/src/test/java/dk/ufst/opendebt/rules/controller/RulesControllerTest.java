package dk.ufst.opendebt.rules.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.rules.model.*;
import dk.ufst.opendebt.rules.service.RulesService;

@WebMvcTest(RulesController.class)
class RulesControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private RulesService rulesService;

  private static final UUID DEBT_ID = UUID.randomUUID();

  @Test
  @WithMockUser(roles = "SERVICE")
  void evaluateReadiness_returnsResult() throws Exception {
    DebtReadinessResult result =
        DebtReadinessResult.builder()
            .debtId(DEBT_ID)
            .ready(true)
            .status(DebtReadinessResult.ReadinessStatus.READY_FOR_COLLECTION)
            .build();
    when(rulesService.evaluateReadiness(any())).thenReturn(result);

    DebtReadinessRequest request =
        DebtReadinessRequest.builder()
            .debtId(DEBT_ID)
            .principalAmount(BigDecimal.valueOf(1000))
            .build();

    mockMvc
        .perform(
            post("/api/v1/rules/readiness/evaluate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.debtId").value(DEBT_ID.toString()))
        .andExpect(jsonPath("$.ready").value(true));
  }

  @Test
  @WithMockUser(roles = "CASEWORKER")
  void calculateInterest_returnsResult() throws Exception {
    InterestCalculationResult result =
        InterestCalculationResult.builder()
            .interestAmount(BigDecimal.valueOf(150))
            .interestRate(BigDecimal.valueOf(8.05))
            .daysCalculated(30)
            .build();
    when(rulesService.calculateInterest(any())).thenReturn(result);

    InterestCalculationRequest request =
        InterestCalculationRequest.builder()
            .debtTypeCode("TAX")
            .principalAmount(BigDecimal.valueOf(10000))
            .daysPastDue(30)
            .build();

    mockMvc
        .perform(
            post("/api/v1/rules/interest/calculate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interestAmount").value(150));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void evaluatePriority_returnsResult() throws Exception {
    CollectionPriorityResult result =
        CollectionPriorityResult.builder()
            .debtId(DEBT_ID)
            .priorityRank(CollectionPriorityResult.PRIORITY_TAX)
            .build();
    when(rulesService.determineCollectionPriority(any())).thenReturn(result);

    CollectionPriorityRequest request =
        CollectionPriorityRequest.builder()
            .debtId(DEBT_ID)
            .debtTypeCode("TAX")
            .totalAmount(BigDecimal.valueOf(5000))
            .build();

    mockMvc
        .perform(
            post("/api/v1/rules/priority/evaluate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.priorityRank").value(2));
  }

  @Test
  @WithMockUser(roles = "SERVICE")
  void sortByPriority_returnsSortedList() throws Exception {
    List<CollectionPriorityResult> results =
        List.of(
            CollectionPriorityResult.builder()
                .debtId(DEBT_ID)
                .priorityRank(CollectionPriorityResult.PRIORITY_TAX)
                .build());
    when(rulesService.sortByCollectionPriority(anyList())).thenReturn(results);

    List<CollectionPriorityRequest> requests =
        List.of(
            CollectionPriorityRequest.builder()
                .debtId(DEBT_ID)
                .debtTypeCode("TAX")
                .totalAmount(BigDecimal.valueOf(5000))
                .build());

    mockMvc
        .perform(
            post("/api/v1/rules/priority/sort")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].priorityRank").value(2));
  }

  @Test
  void evaluateReadiness_unauthorized_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/rules/readiness/evaluate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized());
  }
}
