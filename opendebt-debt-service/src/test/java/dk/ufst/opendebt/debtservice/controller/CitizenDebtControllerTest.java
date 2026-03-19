package dk.ufst.opendebt.debtservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import dk.ufst.opendebt.debtservice.dto.CitizenDebtItemDto;
import dk.ufst.opendebt.debtservice.dto.CitizenDebtSummaryResponse;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.service.CitizenDebtService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(
    properties = {
      "spring.cloud.config.enabled=false",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
      "management.tracing.enabled=false"
    })
class CitizenDebtControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CitizenDebtService citizenDebtService;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  private static final UUID PERSON_ID = UUID.randomUUID();
  private static final UUID DEBT_ID = UUID.randomUUID();

  @Test
  void getDebtSummary_withPersonId_returnsOk() throws Exception {
    setupSecurityContextWithPersonId(PERSON_ID);

    CitizenDebtItemDto debtItem =
        CitizenDebtItemDto.builder()
            .debtId(DEBT_ID)
            .debtTypeCode("RESTSKAT")
            .debtTypeName("Restskat")
            .principalAmount(new BigDecimal("10000"))
            .outstandingAmount(new BigDecimal("8000"))
            .interestAmount(new BigDecimal("500"))
            .feesAmount(new BigDecimal("100"))
            .dueDate(LocalDate.now().minusMonths(1))
            .status("ACTIVE")
            .lifecycleState("RESTANCE")
            .build();

    CitizenDebtSummaryResponse response =
        CitizenDebtSummaryResponse.builder()
            .debts(List.of(debtItem))
            .totalOutstandingAmount(new BigDecimal("8000"))
            .totalDebtCount(1)
            .pageNumber(0)
            .pageSize(20)
            .totalPages(1)
            .totalElements(1L)
            .build();

    when(citizenDebtService.getDebtSummary(eq(PERSON_ID), eq(null), any())).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/citizen/debts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalDebtCount").value(1))
        .andExpect(jsonPath("$.totalOutstandingAmount").value(8000))
        .andExpect(jsonPath("$.debts[0].debtId").value(DEBT_ID.toString()))
        .andExpect(jsonPath("$.debts[0].debtTypeCode").value("RESTSKAT"))
        .andExpect(jsonPath("$.debts[0].status").value("ACTIVE"));
  }

  @Test
  void getDebtSummary_withStatusFilter_returnsFiltered() throws Exception {
    setupSecurityContextWithPersonId(PERSON_ID);

    CitizenDebtSummaryResponse response =
        CitizenDebtSummaryResponse.builder()
            .debts(List.of())
            .totalOutstandingAmount(BigDecimal.ZERO)
            .totalDebtCount(0)
            .pageNumber(0)
            .pageSize(20)
            .totalPages(0)
            .totalElements(0L)
            .build();

    when(citizenDebtService.getDebtSummary(eq(PERSON_ID), eq(DebtEntity.DebtStatus.ACTIVE), any()))
        .thenReturn(response);

    mockMvc
        .perform(get("/api/v1/citizen/debts").param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalDebtCount").value(0));
  }

  @Test
  void getDebtSummary_withPagination_returnsPage() throws Exception {
    setupSecurityContextWithPersonId(PERSON_ID);

    CitizenDebtSummaryResponse response =
        CitizenDebtSummaryResponse.builder()
            .debts(List.of())
            .totalOutstandingAmount(BigDecimal.ZERO)
            .totalDebtCount(0)
            .pageNumber(1)
            .pageSize(10)
            .totalPages(1)
            .totalElements(0L)
            .build();

    when(citizenDebtService.getDebtSummary(eq(PERSON_ID), eq(null), any())).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/citizen/debts").param("page", "1").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pageNumber").value(1))
        .andExpect(jsonPath("$.pageSize").value(10));
  }

  @Test
  void getDebtSummary_sizeOver100_limitsTo100() throws Exception {
    setupSecurityContextWithPersonId(PERSON_ID);

    CitizenDebtSummaryResponse response =
        CitizenDebtSummaryResponse.builder()
            .debts(List.of())
            .totalOutstandingAmount(BigDecimal.ZERO)
            .totalDebtCount(0)
            .pageNumber(0)
            .pageSize(100) // Limited to 100
            .totalPages(0)
            .totalElements(0L)
            .build();

    when(citizenDebtService.getDebtSummary(eq(PERSON_ID), eq(null), any())).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/citizen/debts").param("size", "200")) // Request 200
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pageSize").value(100)); // Limited to 100
  }

  @Test
  void getDebtSummary_invalidStatus_returnsBadRequest() throws Exception {
    setupSecurityContextWithPersonId(PERSON_ID);

    mockMvc
        .perform(get("/api/v1/citizen/debts").param("status", "INVALID_STATUS"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getDebtSummary_noPersonIdInContext_returnsInternalServerError() throws Exception {
    // Don't set person_id in context

    mockMvc.perform(get("/api/v1/citizen/debts")).andExpect(status().isInternalServerError());
  }

  /**
   * Helper to set up security context with person_id in authentication details (simulates what the
   * OAuth2 success handler would do).
   */
  private void setupSecurityContextWithPersonId(UUID personId) {
    Map<String, Object> details = Map.of("person_id", personId);
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "citizen-user", null, List.of(new SimpleGrantedAuthority("ROLE_CITIZEN")));
    auth.setDetails(details);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
