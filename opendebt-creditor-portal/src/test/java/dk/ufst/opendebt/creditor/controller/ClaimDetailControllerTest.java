package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.dto.*;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class ClaimDetailControllerTest {

  private static final UUID TEST_CREDITOR_ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TEST_CLAIM_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");

  @Mock private DebtServiceClient debtServiceClient;
  @Mock private CreditorServiceClient creditorServiceClient;
  @Mock private PortalSessionService portalSessionService;
  @Mock private MessageSource messageSource;

  @InjectMocks private ClaimDetailController controller;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  @Test
  void claimDetail_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.claimDetail(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void claimDetail_returnsDetailView_withClaimData() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(buildSingleDebtorClaim());

    Model model = new ConcurrentModel();
    String viewName = controller.claimDetail(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("claims/detail");
    assertThat(model.getAttribute("claim")).isNotNull();
    assertThat(model.getAttribute("claimId")).isEqualTo(TEST_CLAIM_ID);
    assertThat(model.getAttribute("singleDebtor")).isEqualTo(true);
    assertThat(model.getAttribute("currentPage")).isEqualTo("claims-recovery");
  }

  @Test
  void claimDetail_setsMultiDebtor_whenDebtorCountGreaterThanOne() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ClaimDetailDto multiDebtorClaim = buildMultiDebtorClaim();
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(multiDebtorClaim);

    Model model = new ConcurrentModel();
    controller.claimDetail(TEST_CLAIM_ID, model, session);

    assertThat(model.getAttribute("singleDebtor")).isEqualTo(false);
  }

  @Test
  void claimDetail_showsServiceError_whenBackendFails() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.getClaimDetail(any()))
        .thenThrow(new RuntimeException("Connection refused"));
    when(messageSource.getMessage(eq("claim.detail.error.service"), any(), any(Locale.class)))
        .thenReturn("Service error occurred.");

    Model model = new ConcurrentModel();
    String viewName = controller.claimDetail(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("claims/detail");
    assertThat(model.getAttribute("serviceError")).isEqualTo("Service error occurred.");
    assertThat(model.getAttribute("claim")).isNull();
  }

  @Test
  void claimDetail_showsNotFoundError_whenClaimIsNull() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.getClaimDetail(any())).thenReturn(null);
    when(messageSource.getMessage(eq("claim.detail.error.notfound"), any(), any(Locale.class)))
        .thenReturn("Claim not found.");

    Model model = new ConcurrentModel();
    String viewName = controller.claimDetail(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("claims/detail");
    assertThat(model.getAttribute("serviceError")).isEqualTo("Claim not found.");
  }

  @Test
  void claimDetail_censorsCprNumbers() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ClaimDetailDto claim = buildSingleDebtorClaim();
    claim.setDebtors(
        List.of(
            DebtorInfoDto.builder().identifierType("CPR").identifier("0101901234").build(),
            DebtorInfoDto.builder().identifierType("CVR").identifier("12345678").build()));
    claim.setDebtorCount(2);
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(claim);

    Model model = new ConcurrentModel();
    controller.claimDetail(TEST_CLAIM_ID, model, session);

    ClaimDetailDto result = (ClaimDetailDto) model.getAttribute("claim");
    assertThat(result).isNotNull();
    assertThat(result.getDebtors().get(0).getIdentifier()).isEqualTo("010190****");
    assertThat(result.getDebtors().get(1).getIdentifier()).isEqualTo("12345678");
  }

  @Test
  void claimDetail_handlesZeroBalanceExpired() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ClaimDetailDto claim =
        ClaimDetailDto.builder()
            .claimId(TEST_CLAIM_ID)
            .zeroBalanceExpired(true)
            .debtorCount(1)
            .build();
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(claim);

    Model model = new ConcurrentModel();
    String viewName = controller.claimDetail(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("claims/detail");
    ClaimDetailDto result = (ClaimDetailDto) model.getAttribute("claim");
    assertThat(result).isNotNull();
    assertThat(result.isZeroBalanceExpired()).isTrue();
  }

  @Test
  void claimDetail_includesWriteUps() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ClaimDetailDto claim = buildSingleDebtorClaim();
    claim.setWriteUps(
        List.of(
            WriteUpDto.builder()
                .actionId("AKT-001")
                .formType("FORM-A")
                .reason("Adjustment")
                .amount(new BigDecimal("1000.00"))
                .effectiveDate(LocalDate.of(2025, 3, 1))
                .annulled(false)
                .build(),
            WriteUpDto.builder()
                .actionId("AKT-002")
                .formType("FORM-B")
                .reason("Correction")
                .amount(new BigDecimal("500.00"))
                .effectiveDate(LocalDate.of(2025, 4, 1))
                .annulled(true)
                .build()));
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(claim);

    Model model = new ConcurrentModel();
    controller.claimDetail(TEST_CLAIM_ID, model, session);

    ClaimDetailDto result = (ClaimDetailDto) model.getAttribute("claim");
    assertThat(result).isNotNull();
    assertThat(result.getWriteUps()).hasSize(2);
    assertThat(result.getWriteUps().get(1).isAnnulled()).isTrue();
  }

  @Test
  void claimDetail_includesFinancialBreakdown() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ClaimDetailDto claim = buildSingleDebtorClaim();
    claim.setFinancialBreakdown(
        List.of(
            FinancialBreakdownRowDto.builder()
                .category("HOVEDFORDRING")
                .originalAmount(new BigDecimal("50000.00"))
                .writeOffAmount(new BigDecimal("0.00"))
                .paymentAmount(new BigDecimal("10000.00"))
                .balance(new BigDecimal("40000.00"))
                .build(),
            FinancialBreakdownRowDto.builder()
                .category("INDDRIVELSESRENTER")
                .originalAmount(new BigDecimal("2000.00"))
                .writeOffAmount(BigDecimal.ZERO)
                .paymentAmount(BigDecimal.ZERO)
                .balance(new BigDecimal("2000.00"))
                .build()));
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(claim);

    Model model = new ConcurrentModel();
    controller.claimDetail(TEST_CLAIM_ID, model, session);

    ClaimDetailDto result = (ClaimDetailDto) model.getAttribute("claim");
    assertThat(result).isNotNull();
    assertThat(result.getFinancialBreakdown()).hasSize(2);
  }

  @Test
  void claimDetail_includesRelatedClaims() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ClaimDetailDto claim = buildSingleDebtorClaim();
    UUID relatedId = UUID.randomUUID();
    claim.setRelatedClaims(
        List.of(
            RelatedClaimDto.builder()
                .claimId(relatedId)
                .claimType("SKAT")
                .claimCategory("UNDERFORDRING")
                .balance(new BigDecimal("5000.00"))
                .status("IN_RECOVERY")
                .build()));
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(claim);

    Model model = new ConcurrentModel();
    controller.claimDetail(TEST_CLAIM_ID, model, session);

    ClaimDetailDto result = (ClaimDetailDto) model.getAttribute("claim");
    assertThat(result).isNotNull();
    assertThat(result.getRelatedClaims()).hasSize(1);
    assertThat(result.getRelatedClaims().get(0).getClaimId()).isEqualTo(relatedId);
  }

  @Test
  void claimDetail_includesDecisionsForSingleDebtor() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ClaimDetailDto claim = buildSingleDebtorClaim();
    claim.setDecisions(
        List.of(
            DecisionDto.builder()
                .type("DOM")
                .date(LocalDate.of(2025, 6, 15))
                .description("Court ruling")
                .build()));
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(claim);

    Model model = new ConcurrentModel();
    controller.claimDetail(TEST_CLAIM_ID, model, session);

    ClaimDetailDto result = (ClaimDetailDto) model.getAttribute("claim");
    assertThat(result).isNotNull();
    assertThat(result.getDecisions()).hasSize(1);
    assertThat(model.getAttribute("singleDebtor")).isEqualTo(true);
  }

  private ClaimDetailDto buildSingleDebtorClaim() {
    return ClaimDetailDto.builder()
        .claimId(TEST_CLAIM_ID)
        .claimType("SKAT")
        .claimCategory("HOVEDFORDRING")
        .creditorDescription("Tax claim 2024")
        .receivedDate(LocalDate.of(2025, 1, 15))
        .periodFrom(LocalDate.of(2024, 1, 1))
        .periodTo(LocalDate.of(2024, 12, 31))
        .incorporationDate(LocalDate.of(2024, 6, 1))
        .dueDate(LocalDate.of(2025, 2, 1))
        .limitationDate(LocalDate.of(2030, 1, 1))
        .lastTimelyPaymentDate(LocalDate.of(2025, 1, 31))
        .obligationId("OBL-001")
        .creditorReference("REF-001")
        .interestRule("STANDARD")
        .interestRate(new BigDecimal("8.05"))
        .totalDebt(new BigDecimal("52000.00"))
        .originalPrincipal(new BigDecimal("50000.00"))
        .receivedAmount(new BigDecimal("10000.00"))
        .claimBalance(new BigDecimal("42000.00"))
        .totalCreditorBalance(new BigDecimal("42000.00"))
        .amountSentForRecovery(new BigDecimal("50000.00"))
        .amountSentForRecoveryWithWriteUps(new BigDecimal("51000.00"))
        .debtorCount(1)
        .debtors(
            List.of(DebtorInfoDto.builder().identifierType("CPR").identifier("0101901234").build()))
        .zeroBalanceExpired(false)
        .build();
  }

  private ClaimDetailDto buildMultiDebtorClaim() {
    return ClaimDetailDto.builder()
        .claimId(TEST_CLAIM_ID)
        .claimType("SKAT")
        .claimCategory("HOVEDFORDRING")
        .receivedDate(LocalDate.of(2025, 1, 15))
        .incorporationDate(LocalDate.of(2024, 6, 1))
        .obligationId("OBL-002")
        .creditorReference("REF-002")
        .debtorCount(3)
        .debtors(
            List.of(
                DebtorInfoDto.builder().identifierType("CPR").identifier("0101901234").build(),
                DebtorInfoDto.builder().identifierType("CVR").identifier("12345678").build(),
                DebtorInfoDto.builder().identifierType("SE").identifier("87654321").build()))
        .zeroBalanceExpired(false)
        .build();
  }
}
