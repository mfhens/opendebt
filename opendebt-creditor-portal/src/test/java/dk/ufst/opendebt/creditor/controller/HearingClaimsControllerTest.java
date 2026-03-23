package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.client.RestPage;
import dk.ufst.opendebt.creditor.dto.HearingApproveRequestDto;
import dk.ufst.opendebt.creditor.dto.HearingClaimDetailDto;
import dk.ufst.opendebt.creditor.dto.HearingClaimListItemDto;
import dk.ufst.opendebt.creditor.dto.HearingDebtorErrorDto;
import dk.ufst.opendebt.creditor.dto.HearingWithdrawRequestDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class HearingClaimsControllerTest {

  private static final UUID TEST_CREDITOR_ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TEST_CLAIM_ID = UUID.fromString("00000000-0000-0000-0000-000000050001");

  @Mock private DebtServiceClient debtServiceClient;
  @Mock private PortalSessionService portalSessionService;
  @Mock private MessageSource messageSource;

  @InjectMocks private HearingClaimsController controller;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  // --- Hearing list tests ---

  @Test
  void hearingList_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.hearingList(model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void hearingList_returnsHearingListView() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    Model model = new ConcurrentModel();
    String viewName = controller.hearingList(model, session);

    assertThat(viewName).isEqualTo("claims/hearing-list");
    assertThat(model.getAttribute("currentPage")).isEqualTo("claims-hearing");
    assertThat(model.getAttribute("listType")).isEqualTo("hearing");
  }

  @Test
  void hearingTableFragment_returnsClaimsWithData() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    List<HearingClaimListItemDto> claimList = List.of(buildHearingClaimListItem("CVR", "12345678"));
    RestPage<HearingClaimListItemDto> page = new RestPage<>(claimList, 0, 20, 1, 1);
    when(debtServiceClient.listHearingClaims(eq(TEST_CREDITOR_ORG_ID), any())).thenReturn(page);

    Model model = new ConcurrentModel();
    String viewName =
        controller.hearingTableFragment(0, 20, null, "asc", null, null, null, null, model, session);

    assertThat(viewName).isEqualTo("claims/fragments/hearing-table :: hearingTable");
    @SuppressWarnings("unchecked")
    List<HearingClaimListItemDto> claims =
        (List<HearingClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims).hasSize(1);
    assertThat(model.getAttribute("listType")).isEqualTo("hearing");
  }

  @Test
  void hearingTableFragment_returnsEmptyList_whenServiceUnavailable() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.listHearingClaims(any(), any()))
        .thenThrow(new RuntimeException("Connection refused"));

    Model model = new ConcurrentModel();
    String viewName =
        controller.hearingTableFragment(0, 20, null, "asc", null, null, null, null, model, session);

    assertThat(viewName).isEqualTo("claims/fragments/hearing-table :: hearingTable");
    @SuppressWarnings("unchecked")
    List<HearingClaimListItemDto> claims =
        (List<HearingClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims).isEmpty();
  }

  @Test
  void hearingTableFragment_censorsCprNumbers() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    List<HearingClaimListItemDto> claimList =
        List.of(buildHearingClaimListItem("CPR", "0101901234"));
    RestPage<HearingClaimListItemDto> page = new RestPage<>(claimList, 0, 20, 1, 1);
    when(debtServiceClient.listHearingClaims(eq(TEST_CREDITOR_ORG_ID), any())).thenReturn(page);

    Model model = new ConcurrentModel();
    controller.hearingTableFragment(0, 20, null, "asc", null, null, null, null, model, session);

    @SuppressWarnings("unchecked")
    List<HearingClaimListItemDto> claims =
        (List<HearingClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims).hasSize(1);
    assertThat(claims.get(0).getDebtorIdentifier()).isEqualTo("010190****");
  }

  @Test
  void hearingTableFragment_doesNotCensorCvrNumbers() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    List<HearingClaimListItemDto> claimList = List.of(buildHearingClaimListItem("CVR", "12345678"));
    RestPage<HearingClaimListItemDto> page = new RestPage<>(claimList, 0, 20, 1, 1);
    when(debtServiceClient.listHearingClaims(eq(TEST_CREDITOR_ORG_ID), any())).thenReturn(page);

    Model model = new ConcurrentModel();
    controller.hearingTableFragment(0, 20, null, "asc", null, null, null, null, model, session);

    @SuppressWarnings("unchecked")
    List<HearingClaimListItemDto> claims =
        (List<HearingClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims.get(0).getDebtorIdentifier()).isEqualTo("12345678");
  }

  // --- Hearing detail tests ---

  @Test
  void hearingDetail_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.hearingDetail(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void hearingDetail_returnsDetailView_withClaimData() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    HearingClaimDetailDto detail = buildHearingClaimDetail("OPSKRIVNING_REGULERING");
    when(debtServiceClient.getHearingClaimDetail(TEST_CLAIM_ID)).thenReturn(detail);

    Model model = new ConcurrentModel();
    String viewName = controller.hearingDetail(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("claims/hearing-detail");
    assertThat(model.getAttribute("claim")).isNotNull();
    assertThat(model.getAttribute("isWriteUp")).isEqualTo(true);
    assertThat(model.getAttribute("showChangedPrincipal")).isEqualTo(false);
    assertThat(model.getAttribute("claimId")).isEqualTo(TEST_CLAIM_ID);
  }

  @Test
  void hearingDetail_showsChangedPrincipal_forFejlagtigHovedstol() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    HearingClaimDetailDto detail = buildHearingClaimDetail("FEJLAGTIG_HOVEDSTOL_INDBERETNING");
    when(debtServiceClient.getHearingClaimDetail(TEST_CLAIM_ID)).thenReturn(detail);

    Model model = new ConcurrentModel();
    controller.hearingDetail(TEST_CLAIM_ID, model, session);

    assertThat(model.getAttribute("isWriteUp")).isEqualTo(true);
    assertThat(model.getAttribute("showChangedPrincipal")).isEqualTo(true);
  }

  @Test
  void hearingDetail_noWriteUp_forNonWriteUpActionCode() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    HearingClaimDetailDto detail = buildHearingClaimDetail("NORMAL_SUBMISSION");
    when(debtServiceClient.getHearingClaimDetail(TEST_CLAIM_ID)).thenReturn(detail);

    Model model = new ConcurrentModel();
    controller.hearingDetail(TEST_CLAIM_ID, model, session);

    assertThat(model.getAttribute("isWriteUp")).isEqualTo(false);
    assertThat(model.getAttribute("showChangedPrincipal")).isEqualTo(false);
  }

  @Test
  void hearingDetail_handlesServiceError() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.getHearingClaimDetail(TEST_CLAIM_ID))
        .thenThrow(new RuntimeException("Service unavailable"));
    when(messageSource.getMessage(eq("hearing.detail.error.service"), any(), any()))
        .thenReturn("Error loading hearing");

    Model model = new ConcurrentModel();
    String viewName = controller.hearingDetail(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("claims/hearing-detail");
    assertThat(model.getAttribute("serviceError")).isEqualTo("Error loading hearing");
    assertThat(model.getAttribute("claim")).isNull();
  }

  @Test
  void hearingDetail_censorsCprInDebtorList() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    HearingClaimDetailDto detail = buildHearingClaimDetail("NORMAL_SUBMISSION");
    detail.setDebtorsWithErrors(
        List.of(
            HearingDebtorErrorDto.builder()
                .debtorType("CPR")
                .debtorIdentifier("0101901234")
                .errorTypes(List.of("INVALID_ADDRESS"))
                .build()));
    when(debtServiceClient.getHearingClaimDetail(TEST_CLAIM_ID)).thenReturn(detail);

    Model model = new ConcurrentModel();
    controller.hearingDetail(TEST_CLAIM_ID, model, session);

    HearingClaimDetailDto result = (HearingClaimDetailDto) model.getAttribute("claim");
    assertThat(result).isNotNull();
    assertThat(result.getDebtorsWithErrors().get(0).getDebtorIdentifier()).isEqualTo("010190****");
  }

  // --- Approve tests ---

  @Test
  void approveHearing_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    HearingApproveRequestDto form = new HearingApproveRequestDto();
    form.setJustification("test");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "approveForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.approveHearing(
            TEST_CLAIM_ID, form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void approveHearing_redirectsOnSuccess() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(messageSource.getMessage(eq("hearing.approve.success"), any(), any()))
        .thenReturn("Approved");

    HearingApproveRequestDto form = new HearingApproveRequestDto();
    form.setJustification("Verified manually");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "approveForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.approveHearing(
            TEST_CLAIM_ID, form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("redirect:/fordringer/hoering/" + TEST_CLAIM_ID);
    verify(debtServiceClient).approveHearingClaim(TEST_CLAIM_ID, form);
  }

  @Test
  void approveHearing_reloadsDetailOnValidationError() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.getHearingClaimDetail(TEST_CLAIM_ID)).thenReturn(null);
    when(messageSource.getMessage(eq("hearing.detail.error.notfound"), any(), any()))
        .thenReturn("Not found");

    HearingApproveRequestDto form = new HearingApproveRequestDto();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "approveForm");
    bindingResult.rejectValue("justification", "NotBlank", "required");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.approveHearing(
            TEST_CLAIM_ID, form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("claims/hearing-detail");
    verify(debtServiceClient, never()).approveHearingClaim(any(), any());
  }

  @Test
  void approveHearing_handlesBackendError() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    doThrow(new RuntimeException("Backend error"))
        .when(debtServiceClient)
        .approveHearingClaim(any(), any());
    when(debtServiceClient.getHearingClaimDetail(TEST_CLAIM_ID)).thenReturn(null);
    when(messageSource.getMessage(eq("hearing.approve.error"), any(), any()))
        .thenReturn("Approve failed");
    when(messageSource.getMessage(eq("hearing.detail.error.notfound"), any(), any()))
        .thenReturn("Not found");

    HearingApproveRequestDto form = new HearingApproveRequestDto();
    form.setJustification("test");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "approveForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.approveHearing(
            TEST_CLAIM_ID, form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("claims/hearing-detail");
    assertThat(model.getAttribute("actionError")).isEqualTo("Approve failed");
  }

  // --- Withdraw tests ---

  @Test
  void withdrawHearing_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    HearingWithdrawRequestDto form = new HearingWithdrawRequestDto();
    form.setReason("test");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "withdrawForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.withdrawHearing(
            TEST_CLAIM_ID, form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void withdrawHearing_redirectsOnSuccess() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(messageSource.getMessage(eq("hearing.withdraw.success"), any(), any()))
        .thenReturn("Withdrawn");

    HearingWithdrawRequestDto form = new HearingWithdrawRequestDto();
    form.setReason("Submitted in error");
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "withdrawForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.withdrawHearing(
            TEST_CLAIM_ID, form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("redirect:/fordringer/hoering/" + TEST_CLAIM_ID);
    verify(debtServiceClient).withdrawHearingClaim(TEST_CLAIM_ID, form);
  }

  @Test
  void withdrawHearing_reloadsDetailOnValidationError() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.getHearingClaimDetail(TEST_CLAIM_ID)).thenReturn(null);
    when(messageSource.getMessage(eq("hearing.detail.error.notfound"), any(), any()))
        .thenReturn("Not found");

    HearingWithdrawRequestDto form = new HearingWithdrawRequestDto();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "withdrawForm");
    bindingResult.rejectValue("reason", "NotBlank", "required");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.withdrawHearing(
            TEST_CLAIM_ID, form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("claims/hearing-detail");
    verify(debtServiceClient, never()).withdrawHearingClaim(any(), any());
  }

  // --- Write-up action code recognition ---

  @Test
  void hearingDetail_recognizesOpskrivningOmgjort() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    HearingClaimDetailDto detail =
        buildHearingClaimDetail("OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING");
    when(debtServiceClient.getHearingClaimDetail(TEST_CLAIM_ID)).thenReturn(detail);

    Model model = new ConcurrentModel();
    controller.hearingDetail(TEST_CLAIM_ID, model, session);

    assertThat(model.getAttribute("isWriteUp")).isEqualTo(true);
    assertThat(model.getAttribute("showChangedPrincipal")).isEqualTo(false);
  }

  @Test
  void hearingDetail_recognizesOpskrivningAnnulleret() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    HearingClaimDetailDto detail =
        buildHearingClaimDetail("OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING");
    when(debtServiceClient.getHearingClaimDetail(TEST_CLAIM_ID)).thenReturn(detail);

    Model model = new ConcurrentModel();
    controller.hearingDetail(TEST_CLAIM_ID, model, session);

    assertThat(model.getAttribute("isWriteUp")).isEqualTo(true);
    assertThat(model.getAttribute("showChangedPrincipal")).isEqualTo(false);
  }

  // --- Helper builders ---

  private HearingClaimListItemDto buildHearingClaimListItem(String debtorType, String debtorId) {
    return HearingClaimListItemDto.builder()
        .claimId(UUID.randomUUID())
        .reportingTimestamp(LocalDateTime.of(2025, 1, 15, 10, 30))
        .debtorType(debtorType)
        .debtorIdentifier(debtorId)
        .debtorCount(1)
        .creditorReference("REF-001")
        .claimTypeName("SKAT")
        .errorDescription("Invalid address")
        .errorCount(1)
        .hearingStatus("I_HOERING")
        .caseId(UUID.randomUUID())
        .actionCode("OPSKRIVNING_REGULERING")
        .build();
  }

  private HearingClaimDetailDto buildHearingClaimDetail(String actionCode) {
    return HearingClaimDetailDto.builder()
        .claimId(TEST_CLAIM_ID)
        .claimStatusCode("HEARING")
        .claimStatusText("I høring")
        .caseId(UUID.randomUUID())
        .actionId("AKT-001")
        .creditorReference("REF-001")
        .mainClaimId(UUID.randomUUID())
        .claimTypeName("SKAT")
        .creditorDescription("Test creditor")
        .reportingTimestamp(LocalDateTime.of(2025, 1, 15, 10, 30))
        .periodFrom(LocalDate.of(2024, 1, 1))
        .periodTo(LocalDate.of(2024, 12, 31))
        .incorporationDate(LocalDate.of(2024, 6, 1))
        .creditorOrgId(TEST_CREDITOR_ORG_ID)
        .creditorName("SKAT")
        .originalPrincipal(new BigDecimal("45000.00"))
        .receivedAmount(new BigDecimal("45000.00"))
        .actionCode(actionCode)
        .writeUpAmount(new BigDecimal("5000.00"))
        .writeUpReason("Regulation adjustment")
        .referenceActionId("REF-AKT-001")
        .changedOriginalPrincipal(new BigDecimal("50000.00"))
        .debtorsWithErrors(
            List.of(
                HearingDebtorErrorDto.builder()
                    .debtorType("CVR")
                    .debtorIdentifier("12345678")
                    .errorTypes(List.of("INVALID_ADDRESS", "MISSING_PHONE"))
                    .build()))
        .build();
  }
}
