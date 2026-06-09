package dk.ufst.opendebt.debtservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dk.ufst.opendebt.common.audit.AuditContextService;
import dk.ufst.opendebt.debtservice.dto.CitizenDebtItemDto;
import dk.ufst.opendebt.debtservice.dto.CitizenDebtStatus;
import dk.ufst.opendebt.debtservice.dto.CitizenDebtSummaryResponse;
import dk.ufst.opendebt.debtservice.dto.CitizenEffectiveInterestRateDto;
import dk.ufst.opendebt.debtservice.dto.InterestAccrualState;
import dk.ufst.opendebt.debtservice.dto.InterestPauseReasonCode;
import dk.ufst.opendebt.debtservice.dto.WrittenOffReasonCode;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.service.CitizenDebtService;

@WebMvcTest(
    controllers = CitizenDebtController.class,
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
      "management.tracing.enabled=false"
    })
class CitizenDebtControllerTest {

  private static final UUID PERSON_ID = UUID.randomUUID();
  private static final UUID HEADER_PERSON_ID = UUID.randomUUID();
  private static final UUID JWT_PERSON_ID = UUID.randomUUID();
  private static final UUID DEBT_ID = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CitizenDebtService citizenDebtService;
  @MockitoBean private AuditContextService auditContextService;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getDebtSummary_withAuthenticationDetails_returnsOkAndUsesCanonicalPagination()
      throws Exception {
    setupSecurityContextWithDetailsPersonId(PERSON_ID);

    CitizenDebtSummaryResponse response = summaryResponse(1, 10, enrichedDebtItem());
    when(citizenDebtService.getDebtSummary(eq(PERSON_ID), eq(null), any())).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/citizen/debts").param("pageNumber", "1").param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pageNumber").value(1))
        .andExpect(jsonPath("$.pageSize").value(10))
        .andExpect(jsonPath("$.debts[0].creditorDisplayName").value("Skattestyrelsen"));

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(citizenDebtService).getDebtSummary(eq(PERSON_ID), eq(null), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
  }

  @Test
  void getDebtSummary_petition026Projection_includesEnrichedSummaryAndCitizenFields()
      throws Exception {
    setupSecurityContextWithDetailsPersonId(PERSON_ID);

    CitizenDebtSummaryResponse response = summaryResponse(0, 20, enrichedDebtItem());
    when(citizenDebtService.getDebtSummary(eq(PERSON_ID), eq(null), any())).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/citizen/debts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.effectiveInterestRates[0].interestRuleCode").value("INDR_STD"))
        .andExpect(jsonPath("$.debts[0].creditorDisplayName").value("Skattestyrelsen"))
        .andExpect(jsonPath("$.debts[0].citizenStatus").value("IN_COLLECTION"))
        .andExpect(jsonPath("$.debts[0].interestAccrualState").value("ACTIVE"))
        .andExpect(jsonPath("$.debts[0].interestRuleCode").value("INDR_STD"))
        .andExpect(jsonPath("$.debts[0].currentInterestRate").value(0.0575));
  }

  @Test
  void getDebtSummary_petition026Projection_exposesConditionalFieldsWhenApplicable()
      throws Exception {
    setupSecurityContextWithDetailsPersonId(PERSON_ID);

    CitizenDebtItemDto debtItem =
        CitizenDebtItemDto.builder()
            .debtId(DEBT_ID)
            .debtTypeCode("RESTSKAT")
            .debtTypeName("Restskat")
            .creditorDisplayName("Skattestyrelsen")
            .principalAmount(new BigDecimal("10000"))
            .outstandingAmount(BigDecimal.ZERO)
            .interestAmount(BigDecimal.ZERO)
            .feesAmount(BigDecimal.ZERO)
            .dueDate(LocalDate.now().minusMonths(1))
            .status("WRITTEN_OFF")
            .citizenStatus(CitizenDebtStatus.WRITTEN_OFF)
            .statusReasonCode("LIMITATION_EXPIRED")
            .interestAccrualState(InterestAccrualState.ACTIVE)
            .interestRuleCode("INDR_STD")
            .writtenOffReasonCode(WrittenOffReasonCode.LIMITATION_EXPIRED)
            .build();

    when(citizenDebtService.getDebtSummary(eq(PERSON_ID), eq(null), any()))
        .thenReturn(summaryResponse(0, 20, debtItem));

    mockMvc
        .perform(get("/api/v1/citizen/debts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.debts[0].statusReasonCode").value("LIMITATION_EXPIRED"))
        .andExpect(jsonPath("$.debts[0].writtenOffReasonCode").value("LIMITATION_EXPIRED"));
  }

  @Test
  void getDebtSummary_petition026Projection_exposesPauseReasonCode() throws Exception {
    setupSecurityContextWithDetailsPersonId(PERSON_ID);

    CitizenDebtItemDto debtItem =
        CitizenDebtItemDto.builder()
            .debtId(DEBT_ID)
            .debtTypeCode("RESTSKAT")
            .debtTypeName("Restskat")
            .creditorDisplayName("Skattestyrelsen")
            .principalAmount(new BigDecimal("10000"))
            .outstandingAmount(new BigDecimal("8000"))
            .interestAmount(new BigDecimal("500"))
            .feesAmount(new BigDecimal("100"))
            .dueDate(LocalDate.now().minusMonths(1))
            .status("ACTIVE")
            .citizenStatus(CitizenDebtStatus.IN_COLLECTION)
            .interestAccrualState(InterestAccrualState.PAUSED)
            .interestPauseReasonCode(InterestPauseReasonCode.CLAIM_UNCLEAR_DEBTOR_CANNOT_PAY)
            .interestRuleCode("INDR_STD")
            .build();

    when(citizenDebtService.getDebtSummary(eq(PERSON_ID), eq(null), any()))
        .thenReturn(summaryResponse(0, 20, debtItem));

    mockMvc
        .perform(get("/api/v1/citizen/debts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.debts[0].interestAccrualState").value("PAUSED"))
        .andExpect(
            jsonPath("$.debts[0].interestPauseReasonCode")
                .value("CLAIM_UNCLEAR_DEBTOR_CANNOT_PAY"));
  }

  @Test
  void getDebtSummary_jwtPersonIdClaim_takesPrecedenceOverHeaderFallback() throws Exception {
    setupJwtSecurityContext(JWT_PERSON_ID);

    when(citizenDebtService.getDebtSummary(eq(JWT_PERSON_ID), eq(null), any()))
        .thenReturn(summaryResponse(0, 20, enrichedDebtItem()));

    mockMvc
        .perform(
            get("/api/v1/citizen/debts")
                .header("X-Person-Id", HEADER_PERSON_ID.toString())
                .param("pageNumber", "0")
                .param("pageSize", "20"))
        .andExpect(status().isOk());

    verify(citizenDebtService).getDebtSummary(eq(JWT_PERSON_ID), eq(null), any());
  }

  @Test
  void getDebtSummary_headerFallback_supportsCitizenPortalBridgeHeader() throws Exception {
    setupSecurityContextWithoutPersonId();

    when(citizenDebtService.getDebtSummary(eq(HEADER_PERSON_ID), eq(null), any()))
        .thenReturn(summaryResponse(0, 20, enrichedDebtItem()));

    mockMvc
        .perform(get("/api/v1/citizen/debts").header("X-Person-Id", HEADER_PERSON_ID.toString()))
        .andExpect(status().isOk());

    verify(citizenDebtService).getDebtSummary(eq(HEADER_PERSON_ID), eq(null), any());
  }

  @Test
  void getDebtSummary_legacyPaginationAliasesRemainSupported() throws Exception {
    setupSecurityContextWithDetailsPersonId(PERSON_ID);

    when(citizenDebtService.getDebtSummary(eq(PERSON_ID), eq(null), any()))
        .thenReturn(summaryResponse(1, 5, enrichedDebtItem()));

    mockMvc
        .perform(get("/api/v1/citizen/debts").param("page", "1").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pageNumber").value(1))
        .andExpect(jsonPath("$.pageSize").value(5));
  }

  @Test
  void getDebtSummary_withStatusFilter_returnsFiltered() throws Exception {
    setupSecurityContextWithDetailsPersonId(PERSON_ID);

    when(citizenDebtService.getDebtSummary(eq(PERSON_ID), eq(DebtEntity.DebtStatus.ACTIVE), any()))
        .thenReturn(summaryResponse(0, 20, null));

    mockMvc
        .perform(get("/api/v1/citizen/debts").param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalDebtCount").value(0));
  }

  @Test
  void getDebtSummary_invalidStatus_returnsBadRequest() throws Exception {
    setupSecurityContextWithDetailsPersonId(PERSON_ID);

    mockMvc
        .perform(get("/api/v1/citizen/debts").param("status", "INVALID_STATUS"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getDebtSummary_invalidPageSize_returnsBadRequest() throws Exception {
    setupSecurityContextWithDetailsPersonId(PERSON_ID);

    mockMvc
        .perform(get("/api/v1/citizen/debts").param("pageSize", "101"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getDebtSummary_noPersonIdInContext_returnsInternalServerError() throws Exception {
    setupSecurityContextWithoutPersonId();

    mockMvc.perform(get("/api/v1/citizen/debts")).andExpect(status().isInternalServerError());
  }

  private CitizenDebtItemDto enrichedDebtItem() {
    return CitizenDebtItemDto.builder()
        .debtId(DEBT_ID)
        .debtTypeCode("RESTSKAT")
        .debtTypeName("Restskat")
        .creditorDisplayName("Skattestyrelsen")
        .principalAmount(new BigDecimal("10000"))
        .outstandingAmount(new BigDecimal("8000"))
        .interestAmount(new BigDecimal("500"))
        .feesAmount(new BigDecimal("100"))
        .dueDate(LocalDate.now().minusMonths(1))
        .status("ACTIVE")
        .citizenStatus(CitizenDebtStatus.IN_COLLECTION)
        .interestAccrualState(InterestAccrualState.ACTIVE)
        .interestRuleCode("INDR_STD")
        .currentInterestRate(new BigDecimal("0.0575"))
        .build();
  }

  private CitizenDebtSummaryResponse summaryResponse(
      int pageNumber, int pageSize, CitizenDebtItemDto debtItem) {
    List<CitizenDebtItemDto> debts = debtItem != null ? List.of(debtItem) : List.of();
    return CitizenDebtSummaryResponse.builder()
        .debts(debts)
        .totalOutstandingAmount(
            debtItem != null ? debtItem.getOutstandingAmount() : BigDecimal.ZERO)
        .totalDebtCount(debts.size())
        .pageNumber(pageNumber)
        .pageSize(pageSize)
        .totalPages(debts.isEmpty() ? 0 : 1)
        .totalElements(debts.size())
        .effectiveInterestRates(
            List.of(
                CitizenEffectiveInterestRateDto.builder()
                    .interestRuleCode("INDR_STD")
                    .annualRate(new BigDecimal("0.0575"))
                    .validFrom(LocalDate.of(2024, 1, 1))
                    .build()))
        .build();
  }

  private void setupSecurityContextWithDetailsPersonId(UUID personId) {
    Map<String, Object> details = Map.of("person_id", personId);
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            "citizen-user", null, List.of(new SimpleGrantedAuthority("ROLE_CITIZEN")));
    authentication.setDetails(details);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void setupSecurityContextWithoutPersonId() {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            "citizen-user", null, List.of(new SimpleGrantedAuthority("ROLE_CITIZEN")));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void setupJwtSecurityContext(UUID personId) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("person_id", personId.toString())
            .build();
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CITIZEN")));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
