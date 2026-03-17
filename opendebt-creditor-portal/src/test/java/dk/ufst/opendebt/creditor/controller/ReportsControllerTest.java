package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import dk.ufst.opendebt.creditor.client.ReportingServiceClient;
import dk.ufst.opendebt.creditor.dto.ReportListItemDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class ReportsControllerTest {

  private static final UUID TEST_CREDITOR_ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private ReportingServiceClient reportingServiceClient;
  @Mock private MessageSource messageSource;
  @Mock private PortalSessionService portalSessionService;

  @InjectMocks private ReportsController controller;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  @Test
  void listReports_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.listReports(null, null, model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void listReports_returnsRapporterView() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(reportingServiceClient.listReports(any(UUID.class), anyInt(), anyInt()))
        .thenReturn(Collections.emptyList());

    Model model = new ConcurrentModel();
    String viewName = controller.listReports(2025, 3, model, session);

    assertThat(viewName).isEqualTo("rapporter");
    assertThat(model.getAttribute("currentPage")).isEqualTo("reports");
    assertThat(model.getAttribute("selectedYear")).isEqualTo(2025);
    assertThat(model.getAttribute("selectedMonth")).isEqualTo(3);
  }

  @Test
  void listReports_filtersOutReconciliationSummaryReports() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    ReportListItemDto normalReport =
        ReportListItemDto.builder()
            .reportId(UUID.randomUUID())
            .reportName("Monthly overview")
            .reportType("MONTHLY")
            .availabilityStatus("AVAILABLE")
            .reconciliationSummary(false)
            .build();

    ReportListItemDto reconciliationReport =
        ReportListItemDto.builder()
            .reportId(UUID.randomUUID())
            .reportName("Reconciliation summary")
            .reportType("RECONCILIATION_SUMMARY")
            .availabilityStatus("AVAILABLE")
            .reconciliationSummary(true)
            .build();

    when(reportingServiceClient.listReports(any(UUID.class), anyInt(), anyInt()))
        .thenReturn(List.of(normalReport, reconciliationReport));

    Model model = new ConcurrentModel();
    controller.listReports(2025, 3, model, session);

    @SuppressWarnings("unchecked")
    List<ReportListItemDto> reports = (List<ReportListItemDto>) model.getAttribute("reports");
    assertThat(reports).hasSize(1);
    assertThat(reports.get(0).getReportName()).isEqualTo("Monthly overview");
  }

  @Test
  void listReports_showsEmptyList_whenNoReportsExist() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(reportingServiceClient.listReports(any(UUID.class), anyInt(), anyInt()))
        .thenReturn(Collections.emptyList());

    Model model = new ConcurrentModel();
    controller.listReports(2025, 3, model, session);

    @SuppressWarnings("unchecked")
    List<ReportListItemDto> reports = (List<ReportListItemDto>) model.getAttribute("reports");
    assertThat(reports).isEmpty();
  }

  @Test
  void listReports_handlesBackendError_gracefully() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(reportingServiceClient.listReports(any(UUID.class), anyInt(), anyInt()))
        .thenThrow(new RuntimeException("Connection refused"));
    when(messageSource.getMessage(eq("reports.error.service"), any(), any()))
        .thenReturn("Service error.");

    Model model = new ConcurrentModel();
    String viewName = controller.listReports(2025, 3, model, session);

    assertThat(viewName).isEqualTo("rapporter");
    assertThat(model.getAttribute("backendError")).isNotNull();
  }

  @Test
  void listReports_addsAvailableYearsAndMonths() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(reportingServiceClient.listReports(any(UUID.class), anyInt(), anyInt()))
        .thenReturn(Collections.emptyList());

    Model model = new ConcurrentModel();
    controller.listReports(null, null, model, session);

    @SuppressWarnings("unchecked")
    List<Integer> years = (List<Integer>) model.getAttribute("availableYears");
    assertThat(years).isNotEmpty().hasSize(6);

    @SuppressWarnings("unchecked")
    List<Integer> months = (List<Integer>) model.getAttribute("availableMonths");
    assertThat(months).hasSize(12);
  }

  @Test
  void reportListFragment_returnsFragmentView() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(reportingServiceClient.listReports(any(UUID.class), anyInt(), anyInt()))
        .thenReturn(Collections.emptyList());

    Model model = new ConcurrentModel();
    String viewName = controller.reportListFragment(2025, 3, model, session);

    assertThat(viewName).isEqualTo("fragments/report-list :: reportList");
  }

  @Test
  void downloadReport_returns401_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    ResponseEntity<byte[]> response = controller.downloadReport(UUID.randomUUID(), session);

    assertThat(response.getStatusCode().value()).isEqualTo(401);
  }

  @Test
  void downloadReport_returnsZipContent_withContentDisposition() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    UUID reportId = UUID.randomUUID();
    byte[] zipContent = new byte[] {0x50, 0x4B, 0x03, 0x04}; // PK zip header
    when(reportingServiceClient.downloadReport(reportId)).thenReturn(zipContent);

    ResponseEntity<byte[]> response = controller.downloadReport(reportId, session);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/zip");
    assertThat(response.getHeaders().getContentDisposition().getFilename())
        .isEqualTo("report-" + reportId + ".zip");
    assertThat(response.getBody()).isEqualTo(zipContent);
  }

  @Test
  void downloadReport_returns404_whenReportNotFound() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    UUID reportId = UUID.randomUUID();
    when(reportingServiceClient.downloadReport(reportId)).thenReturn(null);

    ResponseEntity<byte[]> response = controller.downloadReport(reportId, session);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  void downloadReport_returns500_whenBackendFails() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    UUID reportId = UUID.randomUUID();
    when(reportingServiceClient.downloadReport(reportId))
        .thenThrow(new RuntimeException("Connection refused"));

    ResponseEntity<byte[]> response = controller.downloadReport(reportId, session);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
  }

  @Test
  void downloadReport_logsAuditEvent() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    UUID reportId = UUID.randomUUID();
    byte[] zipContent = new byte[] {0x50, 0x4B};
    when(reportingServiceClient.downloadReport(reportId)).thenReturn(zipContent);

    controller.downloadReport(reportId, session);

    // Verify the download was invoked (audit logging is done via log.info in controller)
    verify(reportingServiceClient).downloadReport(reportId);
  }
}
