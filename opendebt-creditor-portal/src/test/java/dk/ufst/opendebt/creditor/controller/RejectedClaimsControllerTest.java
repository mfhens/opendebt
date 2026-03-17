package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.client.RestPage;
import dk.ufst.opendebt.creditor.dto.ClaimListItemDto;
import dk.ufst.opendebt.creditor.dto.RejectedClaimDebtorDto;
import dk.ufst.opendebt.creditor.dto.RejectedClaimDetailDto;
import dk.ufst.opendebt.creditor.dto.ValidationErrorDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class RejectedClaimsControllerTest {

  private static final UUID TEST_CREDITOR_ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TEST_CLAIM_ID = UUID.fromString("00000000-0000-0000-0000-000000060001");

  @Mock private DebtServiceClient debtServiceClient;
  @Mock private PortalSessionService portalSessionService;
  @Mock private MessageSource messageSource;

  private RejectedClaimsController controller;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    controller =
        new RejectedClaimsController(debtServiceClient, portalSessionService, messageSource);
    ReflectionTestUtils.setField(controller, "showDebtorDetails", true);
    session = new MockHttpSession();
  }

  @Test
  void rejectedList_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.rejectedList(model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void rejectedList_returnsRejectedListView() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    Model model = new ConcurrentModel();
    String viewName = controller.rejectedList(model, session);

    assertThat(viewName).isEqualTo("claims/rejected-list");
    assertThat(model.getAttribute("currentPage")).isEqualTo("claims-rejected");
    assertThat(model.getAttribute("listType")).isEqualTo("rejected");
  }

  @Test
  void rejectedTableFragment_returnsClaimsWithData() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    List<ClaimListItemDto> claimList = List.of(buildClaimListItem("CVR", "12345678"));
    RestPage<ClaimListItemDto> page = new RestPage<>(claimList, 0, 20, 1, 1);
    when(debtServiceClient.listRejectedClaims(
            eq(TEST_CREDITOR_ORG_ID), anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
        .thenReturn(page);

    Model model = new ConcurrentModel();
    String viewName =
        controller.rejectedTableFragment(
            0, 20, null, "asc", null, null, null, null, model, session);

    assertThat(viewName).isEqualTo("claims/fragments/rejected-claims-table :: rejectedClaimsTable");
    @SuppressWarnings("unchecked")
    List<ClaimListItemDto> claims = (List<ClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims).hasSize(1);
    assertThat(model.getAttribute("listType")).isEqualTo("rejected");
  }

  @Test
  void rejectedTableFragment_returnsEmptyList_whenServiceUnavailable() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.listRejectedClaims(
            any(), anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Connection refused"));

    Model model = new ConcurrentModel();
    String viewName =
        controller.rejectedTableFragment(
            0, 20, null, "asc", null, null, null, null, model, session);

    assertThat(viewName).isEqualTo("claims/fragments/rejected-claims-table :: rejectedClaimsTable");
    @SuppressWarnings("unchecked")
    List<ClaimListItemDto> claims = (List<ClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims).isEmpty();
  }

  @Test
  void rejectedTableFragment_censorsCprNumbers() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    List<ClaimListItemDto> claimList = List.of(buildClaimListItem("CPR", "0101901234"));
    RestPage<ClaimListItemDto> page = new RestPage<>(claimList, 0, 20, 1, 1);
    when(debtServiceClient.listRejectedClaims(
            eq(TEST_CREDITOR_ORG_ID), anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
        .thenReturn(page);

    Model model = new ConcurrentModel();
    controller.rejectedTableFragment(0, 20, null, "asc", null, null, null, null, model, session);

    @SuppressWarnings("unchecked")
    List<ClaimListItemDto> claims = (List<ClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims).hasSize(1);
    assertThat(claims.get(0).getDebtorIdentifier()).isEqualTo("010190****");
  }

  @Test
  void rejectedTableFragment_doesNotCensorCvrNumbers() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    List<ClaimListItemDto> claimList = List.of(buildClaimListItem("CVR", "12345678"));
    RestPage<ClaimListItemDto> page = new RestPage<>(claimList, 0, 20, 1, 1);
    when(debtServiceClient.listRejectedClaims(
            eq(TEST_CREDITOR_ORG_ID), anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
        .thenReturn(page);

    Model model = new ConcurrentModel();
    controller.rejectedTableFragment(0, 20, null, "asc", null, null, null, null, model, session);

    @SuppressWarnings("unchecked")
    List<ClaimListItemDto> claims = (List<ClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims.get(0).getDebtorIdentifier()).isEqualTo("12345678");
  }

  @Test
  void rejectedClaimDetail_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.rejectedClaimDetail(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void rejectedClaimDetail_returnsDetailView_withClaimData() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    RejectedClaimDetailDto detail = buildRejectedClaimDetail();
    when(debtServiceClient.getRejectedClaimDetail(TEST_CLAIM_ID)).thenReturn(detail);

    Model model = new ConcurrentModel();
    String viewName = controller.rejectedClaimDetail(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("claims/rejected-detail");
    assertThat(model.getAttribute("claim")).isNotNull();
    assertThat(model.getAttribute("claimId")).isEqualTo(TEST_CLAIM_ID);
    assertThat(model.getAttribute("showDebtorDetails")).isEqualTo(true);
    assertThat(model.getAttribute("currentPage")).isEqualTo("claims-rejected");
  }

  @Test
  void rejectedClaimDetail_censorsDebtorCprNumbers() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    RejectedClaimDetailDto detail = buildRejectedClaimDetail();
    when(debtServiceClient.getRejectedClaimDetail(TEST_CLAIM_ID)).thenReturn(detail);

    Model model = new ConcurrentModel();
    controller.rejectedClaimDetail(TEST_CLAIM_ID, model, session);

    RejectedClaimDetailDto result = (RejectedClaimDetailDto) model.getAttribute("claim");
    assertThat(result).isNotNull();
    assertThat(result.getDebtors()).hasSize(2);
    // CPR debtor should be censored
    assertThat(result.getDebtors().get(0).getIdentifier()).isEqualTo("010190****");
    // CVR debtor should not be censored
    assertThat(result.getDebtors().get(1).getIdentifier()).isEqualTo("12345678");
  }

  @Test
  void rejectedClaimDetail_handlesServiceError() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.getRejectedClaimDetail(TEST_CLAIM_ID))
        .thenThrow(new RuntimeException("Connection refused"));
    when(messageSource.getMessage(eq("rejected.detail.error.service"), any(), any()))
        .thenReturn("Service error message");

    Model model = new ConcurrentModel();
    String viewName = controller.rejectedClaimDetail(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("claims/rejected-detail");
    assertThat(model.getAttribute("claim")).isNull();
    assertThat(model.getAttribute("serviceError")).isEqualTo("Service error message");
  }

  @Test
  void rejectedClaimDetail_handlesNotFound() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.getRejectedClaimDetail(TEST_CLAIM_ID)).thenReturn(null);
    when(messageSource.getMessage(eq("rejected.detail.error.notfound"), any(), any()))
        .thenReturn("Not found message");

    Model model = new ConcurrentModel();
    String viewName = controller.rejectedClaimDetail(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("claims/rejected-detail");
    assertThat(model.getAttribute("claim")).isNull();
    assertThat(model.getAttribute("serviceError")).isEqualTo("Not found message");
  }

  @Test
  void rejectedClaimDetail_showDebtorDetailsFlag_controlledByConfig() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    RejectedClaimDetailDto detail = buildRejectedClaimDetail();
    when(debtServiceClient.getRejectedClaimDetail(TEST_CLAIM_ID)).thenReturn(detail);

    // Set flag to false
    ReflectionTestUtils.setField(controller, "showDebtorDetails", false);

    Model model = new ConcurrentModel();
    controller.rejectedClaimDetail(TEST_CLAIM_ID, model, session);

    assertThat(model.getAttribute("showDebtorDetails")).isEqualTo(false);
  }

  private ClaimListItemDto buildClaimListItem(String debtorType, String debtorId) {
    return ClaimListItemDto.builder()
        .claimId(UUID.randomUUID())
        .receivedDate(LocalDate.of(2025, 1, 15))
        .debtorType(debtorType)
        .debtorIdentifier(debtorId)
        .debtorCount(1)
        .creditorReference("REF-001")
        .claimTypeName("SKAT")
        .claimStatus("REJECTED")
        .incorporationDate(LocalDate.of(2024, 6, 1))
        .periodFrom(LocalDate.of(2024, 1, 1))
        .periodTo(LocalDate.of(2024, 12, 31))
        .amountSentForRecovery(new BigDecimal("45000.00"))
        .balance(BigDecimal.ZERO)
        .balanceWithInterestAndFees(BigDecimal.ZERO)
        .build();
  }

  private RejectedClaimDetailDto buildRejectedClaimDetail() {
    return RejectedClaimDetailDto.builder()
        .actionStatus("REJECTED")
        .rejectionReason("Validation errors found")
        .claimId(TEST_CLAIM_ID)
        .creditorReference("REF-001")
        .claimType("SKAT")
        .creditorDescription("Test creditor")
        .reportedDate(LocalDate.of(2025, 1, 15))
        .periodFrom(LocalDate.of(2024, 1, 1))
        .periodTo(LocalDate.of(2024, 12, 31))
        .incorporationDate(LocalDate.of(2024, 6, 1))
        .interestRuleNumber("R001")
        .interestRateCode("S001")
        .creditorId("K1")
        .creditorName("Test Fordringshaver")
        .originalAmount(new BigDecimal("50000.00"))
        .claimAmount(new BigDecimal("45000.00"))
        .validationErrors(
            List.of(
                ValidationErrorDto.builder().errorCode(152).description("Ugyldig valuta").build(),
                ValidationErrorDto.builder()
                    .errorCode(411)
                    .description("Forkert fordringsart")
                    .build()))
        .caseworkerRemark("Please correct the currency")
        .debtors(
            List.of(
                RejectedClaimDebtorDto.builder()
                    .identifierType("CPR")
                    .identifier("0101901234")
                    .dueDate(LocalDate.of(2025, 3, 1))
                    .lastTimelyPaymentDate(LocalDate.of(2025, 2, 15))
                    .limitationDate(LocalDate.of(2030, 1, 1))
                    .courtDate(null)
                    .settlementDate(null)
                    .estateProcessing(false)
                    .debtorNote("Test note")
                    .build(),
                RejectedClaimDebtorDto.builder()
                    .identifierType("CVR")
                    .identifier("12345678")
                    .dueDate(LocalDate.of(2025, 3, 1))
                    .lastTimelyPaymentDate(LocalDate.of(2025, 2, 15))
                    .limitationDate(LocalDate.of(2030, 1, 1))
                    .courtDate(LocalDate.of(2025, 6, 1))
                    .settlementDate(LocalDate.of(2025, 5, 15))
                    .estateProcessing(true)
                    .debtorNote(null)
                    .build()))
        .build();
  }
}
