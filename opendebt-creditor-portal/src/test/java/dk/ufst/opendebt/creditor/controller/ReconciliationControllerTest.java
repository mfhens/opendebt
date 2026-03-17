package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import dk.ufst.opendebt.creditor.client.ReconciliationServiceClient;
import dk.ufst.opendebt.creditor.dto.ReconciliationBasisDto;
import dk.ufst.opendebt.creditor.dto.ReconciliationDetailDto;
import dk.ufst.opendebt.creditor.dto.ReconciliationListItemDto;
import dk.ufst.opendebt.creditor.dto.ReconciliationResponseFormDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class ReconciliationControllerTest {

  private static final UUID TEST_CREDITOR_ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TEST_RECONCILIATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000099");

  @Mock private ReconciliationServiceClient reconciliationServiceClient;
  @Mock private MessageSource messageSource;
  @Mock private PortalSessionService portalSessionService;

  @InjectMocks private ReconciliationController controller;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  // --- List tests ---

  @Test
  void list_redirectsToDemoLogin_whenNoSessionCreditor() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.list(null, null, null, null, null, null, null, model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void list_returnsListView_withReconciliations() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    List<ReconciliationListItemDto> items =
        List.of(
            ReconciliationListItemDto.builder()
                .id(TEST_RECONCILIATION_ID)
                .status("ACTIVE")
                .year(2025)
                .month(3)
                .periodEndDate(LocalDate.of(2025, 3, 31))
                .responseSubmitted(false)
                .build());
    when(reconciliationServiceClient.listReconciliations(
            eq(TEST_CREDITOR_ORG_ID), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(items);

    Model model = new ConcurrentModel();
    String viewName = controller.list(null, null, null, null, null, null, null, model, session);

    assertThat(viewName).isEqualTo("reconciliation/list");
    assertThat(model.getAttribute("reconciliations")).isEqualTo(items);
    assertThat(model.getAttribute("currentPage")).isEqualTo("reconciliation");
  }

  @Test
  void list_showsEmptyList_whenBackendFails() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(reconciliationServiceClient.listReconciliations(
            eq(TEST_CREDITOR_ORG_ID), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Connection refused"));
    when(messageSource.getMessage(eq("reconciliation.error.service"), any(), any()))
        .thenReturn("Service error.");

    Model model = new ConcurrentModel();
    String viewName = controller.list(null, null, null, null, null, null, null, model, session);

    assertThat(viewName).isEqualTo("reconciliation/list");
    assertThat(model.getAttribute("backendError")).isNotNull();
    @SuppressWarnings("unchecked")
    List<ReconciliationListItemDto> reconciliations =
        (List<ReconciliationListItemDto>) model.getAttribute("reconciliations");
    assertThat(reconciliations).isEmpty();
  }

  @Test
  void list_passesFilterParameters() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(reconciliationServiceClient.listReconciliations(
            any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of());

    Model model = new ConcurrentModel();
    controller.list("ACTIVE", null, null, null, null, null, null, model, session);

    assertThat(model.getAttribute("filterStatus")).isEqualTo("ACTIVE");
  }

  // --- Detail tests ---

  @Test
  void detail_redirectsToDemoLogin_whenNoSessionCreditor() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.detail(TEST_RECONCILIATION_ID, model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void detail_showsError_whenReconciliationNotFound() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(reconciliationServiceClient.getReconciliationDetail(TEST_RECONCILIATION_ID))
        .thenReturn(null);
    when(messageSource.getMessage(eq("reconciliation.error.notfound"), any(), any()))
        .thenReturn("Not found.");

    Model model = new ConcurrentModel();
    String viewName = controller.detail(TEST_RECONCILIATION_ID, model, session);

    assertThat(viewName).isEqualTo("reconciliation/detail");
    assertThat(model.getAttribute("backendError")).isNotNull();
  }

  @Test
  void detail_showsActiveReconciliationWithBasisData() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ReconciliationDetailDto detail = buildActiveDetail();
    when(reconciliationServiceClient.getReconciliationDetail(TEST_RECONCILIATION_ID))
        .thenReturn(detail);
    ReconciliationBasisDto basis = buildBasis();
    when(reconciliationServiceClient.getReconciliationBasis(TEST_RECONCILIATION_ID))
        .thenReturn(basis);

    Model model = new ConcurrentModel();
    String viewName = controller.detail(TEST_RECONCILIATION_ID, model, session);

    assertThat(viewName).isEqualTo("reconciliation/detail");
    assertThat(model.getAttribute("reconciliation")).isNotNull();
    ReconciliationDetailDto result = (ReconciliationDetailDto) model.getAttribute("reconciliation");
    assertThat(result.getBasis()).isNotNull();
    assertThat(model.getAttribute("basisChecksum")).isNotNull();
  }

  @Test
  void detail_showsClosedReconciliationWithoutBasis() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ReconciliationDetailDto detail = buildClosedDetail();
    when(reconciliationServiceClient.getReconciliationDetail(TEST_RECONCILIATION_ID))
        .thenReturn(detail);

    Model model = new ConcurrentModel();
    String viewName = controller.detail(TEST_RECONCILIATION_ID, model, session);

    assertThat(viewName).isEqualTo("reconciliation/detail");
    ReconciliationDetailDto result = (ReconciliationDetailDto) model.getAttribute("reconciliation");
    assertThat(result.getBasis()).isNull();
    // Should not attempt to load basis for CLOSED
    verify(reconciliationServiceClient, never()).getReconciliationBasis(any());
  }

  // --- Confirmation tests ---

  @Test
  void confirmResponse_rejectsInvalidTotal() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(messageSource.getMessage(eq("reconciliation.validation.total.mismatch"), any(), any()))
        .thenReturn("Mismatch");

    ReconciliationResponseFormDto form =
        ReconciliationResponseFormDto.builder()
            .explainedDifference(new BigDecimal("1000.00"))
            .unexplainedDifference(new BigDecimal("500.00"))
            .totalDifference(new BigDecimal("2000.00"))
            .build();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "responseForm");

    // Set up detail reload mocks
    ReconciliationDetailDto detail = buildActiveDetail();
    when(reconciliationServiceClient.getReconciliationDetail(TEST_RECONCILIATION_ID))
        .thenReturn(detail);
    when(reconciliationServiceClient.getReconciliationBasis(TEST_RECONCILIATION_ID))
        .thenReturn(buildBasis());

    Model model = new ConcurrentModel();
    String viewName =
        controller.confirmResponse(TEST_RECONCILIATION_ID, form, bindingResult, model, session);

    assertThat(viewName).isEqualTo("reconciliation/detail");
    assertThat(bindingResult.hasErrors()).isTrue();
  }

  @Test
  void confirmResponse_showsConfirmation_whenValid() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ReconciliationBasisDto basis = buildBasis();
    String checksum = controller.computeBasisChecksum(basis);

    ReconciliationResponseFormDto form =
        ReconciliationResponseFormDto.builder()
            .explainedDifference(new BigDecimal("1000.00"))
            .unexplainedDifference(new BigDecimal("500.00"))
            .totalDifference(new BigDecimal("1500.00"))
            .basisChecksum(checksum)
            .build();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "responseForm");

    ReconciliationDetailDto detail = buildActiveDetail();
    when(reconciliationServiceClient.getReconciliationDetail(TEST_RECONCILIATION_ID))
        .thenReturn(detail);
    when(reconciliationServiceClient.getReconciliationBasis(TEST_RECONCILIATION_ID))
        .thenReturn(basis);

    Model model = new ConcurrentModel();
    String viewName =
        controller.confirmResponse(TEST_RECONCILIATION_ID, form, bindingResult, model, session);

    assertThat(viewName).isEqualTo("reconciliation/detail");
    assertThat(model.getAttribute("showConfirmation")).isEqualTo(true);
  }

  // --- Submit tests ---

  @Test
  void submitResponse_redirectsOnSuccess() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ReconciliationBasisDto basis = buildBasis();
    String checksum = controller.computeBasisChecksum(basis);

    ReconciliationResponseFormDto form =
        ReconciliationResponseFormDto.builder()
            .explainedDifference(new BigDecimal("1000.00"))
            .unexplainedDifference(new BigDecimal("500.00"))
            .totalDifference(new BigDecimal("1500.00"))
            .basisChecksum(checksum)
            .build();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "responseForm");

    when(reconciliationServiceClient.getReconciliationBasis(TEST_RECONCILIATION_ID))
        .thenReturn(basis);
    when(messageSource.getMessage(eq("reconciliation.submit.success"), any(), any()))
        .thenReturn("Success");

    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    String viewName =
        controller.submitResponse(
            TEST_RECONCILIATION_ID, form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("redirect:/afstemning/" + TEST_RECONCILIATION_ID);
    verify(reconciliationServiceClient)
        .submitReconciliationResponse(eq(TEST_RECONCILIATION_ID), any());
  }

  @Test
  void submitResponse_showsError_whenBackendFails() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ReconciliationBasisDto basis = buildBasis();
    String checksum = controller.computeBasisChecksum(basis);

    ReconciliationResponseFormDto form =
        ReconciliationResponseFormDto.builder()
            .explainedDifference(new BigDecimal("1000.00"))
            .unexplainedDifference(new BigDecimal("500.00"))
            .totalDifference(new BigDecimal("1500.00"))
            .basisChecksum(checksum)
            .build();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "responseForm");

    when(reconciliationServiceClient.getReconciliationBasis(TEST_RECONCILIATION_ID))
        .thenReturn(basis);
    doThrow(new RuntimeException("Connection refused"))
        .when(reconciliationServiceClient)
        .submitReconciliationResponse(any(), any());
    when(messageSource.getMessage(eq("reconciliation.submit.error"), any(), any()))
        .thenReturn("Error");

    // Set up detail reload mocks
    ReconciliationDetailDto detail = buildActiveDetail();
    when(reconciliationServiceClient.getReconciliationDetail(TEST_RECONCILIATION_ID))
        .thenReturn(detail);

    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    String viewName =
        controller.submitResponse(
            TEST_RECONCILIATION_ID, form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("reconciliation/detail");
    assertThat(model.getAttribute("backendError")).isNotNull();
  }

  @Test
  void submitResponse_detectsTamperWhenChecksumMismatch() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    ReconciliationResponseFormDto form =
        ReconciliationResponseFormDto.builder()
            .explainedDifference(new BigDecimal("1000.00"))
            .unexplainedDifference(new BigDecimal("500.00"))
            .totalDifference(new BigDecimal("1500.00"))
            .basisChecksum("tampered-checksum")
            .build();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "responseForm");

    ReconciliationBasisDto basis = buildBasis();
    when(reconciliationServiceClient.getReconciliationBasis(TEST_RECONCILIATION_ID))
        .thenReturn(basis);
    when(messageSource.getMessage(eq("reconciliation.error.tamper"), any(), any()))
        .thenReturn("Tampered");

    // Set up detail reload mocks
    ReconciliationDetailDto detail = buildActiveDetail();
    when(reconciliationServiceClient.getReconciliationDetail(TEST_RECONCILIATION_ID))
        .thenReturn(detail);

    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    String viewName =
        controller.submitResponse(
            TEST_RECONCILIATION_ID, form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("reconciliation/detail");
    assertThat(model.getAttribute("backendError")).isNotNull();
    verify(reconciliationServiceClient, never()).submitReconciliationResponse(any(), any());
  }

  // --- Checksum tests ---

  @Test
  void computeBasisChecksum_returnsConsistentHash() {
    ReconciliationBasisDto basis = buildBasis();
    String checksum1 = controller.computeBasisChecksum(basis);
    String checksum2 = controller.computeBasisChecksum(basis);

    assertThat(checksum1).isNotBlank();
    assertThat(checksum1).isEqualTo(checksum2);
  }

  @Test
  void computeBasisChecksum_returnsEmptyForNull() {
    String checksum = controller.computeBasisChecksum(null);
    assertThat(checksum).isEmpty();
  }

  // --- Helpers ---

  private ReconciliationDetailDto buildActiveDetail() {
    return ReconciliationDetailDto.builder()
        .id(TEST_RECONCILIATION_ID)
        .status("ACTIVE")
        .year(2025)
        .month(3)
        .build();
  }

  private ReconciliationDetailDto buildClosedDetail() {
    return ReconciliationDetailDto.builder()
        .id(TEST_RECONCILIATION_ID)
        .status("CLOSED")
        .year(2025)
        .month(3)
        .build();
  }

  private ReconciliationBasisDto buildBasis() {
    return ReconciliationBasisDto.builder()
        .influxAmount(new BigDecimal("100000.00"))
        .recallAmount(new BigDecimal("5000.00"))
        .writeUpAmount(new BigDecimal("2500.00"))
        .writeDownAmount(new BigDecimal("1000.00"))
        .build();
  }
}
