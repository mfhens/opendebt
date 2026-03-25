package dk.ufst.opendebt.creditor.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import jakarta.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.ufst.opendebt.creditor.client.ReportingServiceClient;
import dk.ufst.opendebt.creditor.dto.ReportListItemDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for the monthly reports page in the creditor portal (petition 037). Handles report
 * browsing by year/month and individual report downloads with audit logging.
 */
@Slf4j
@Controller
@RequestMapping("/rapporter")
@RequiredArgsConstructor
public class ReportsController {

  private static final String MODEL_REPORTS = "reports";

  private final ReportingServiceClient reportingServiceClient;
  private final MessageSource messageSource;
  private final PortalSessionService portalSessionService;

  /** GET /rapporter — displays the reports page with year/month selector and report list. */
  @GetMapping
  public String listReports(
      @RequestParam(name = "year", required = false) Integer year,
      @RequestParam(name = "month", required = false) Integer month,
      Model model,
      HttpSession session) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    LocalDate now = LocalDate.now();
    int selectedYear = (year != null) ? year : now.getYear();
    int selectedMonth = (month != null) ? month : now.getMonthValue();

    model.addAttribute("currentPage", MODEL_REPORTS);
    model.addAttribute("actingCreditorOrgId", actingCreditor);
    addRepresentedCreditorIfPresent(model, session);

    // Provide year/month selector options
    int currentYear = now.getYear();
    List<Integer> availableYears =
        IntStream.rangeClosed(currentYear - 5, currentYear)
            .boxed()
            .sorted((a, b) -> b - a)
            .toList();
    List<Integer> availableMonths = IntStream.rangeClosed(1, 12).boxed().toList();

    model.addAttribute("availableYears", availableYears);
    model.addAttribute("availableMonths", availableMonths);
    model.addAttribute("selectedYear", selectedYear);
    model.addAttribute("selectedMonth", selectedMonth);

    try {
      List<ReportListItemDto> allReports =
          reportingServiceClient.listReports(actingCreditor, selectedYear, selectedMonth);

      // Filter out reconciliation summary files (petition 037 FR-4)
      List<ReportListItemDto> reports =
          allReports.stream().filter(r -> !r.isReconciliationSummary()).toList();

      model.addAttribute(MODEL_REPORTS, reports);
    } catch (Exception ex) {
      log.error("Failed to load reports: {}", ex.getMessage());
      model.addAttribute(
          "backendError",
          messageSource.getMessage("reports.error.service", null, LocaleContextHolder.getLocale()));
      model.addAttribute(MODEL_REPORTS, List.of());
    }

    return "rapporter";
  }

  /**
   * HTMX endpoint that returns only the report list fragment for dynamic period changes.
   *
   * @return the report-list fragment
   */
  @GetMapping("/list")
  public String reportListFragment(
      @RequestParam(name = "year") int year,
      @RequestParam(name = "month") int month,
      Model model,
      HttpSession session) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    try {
      List<ReportListItemDto> allReports =
          reportingServiceClient.listReports(actingCreditor, year, month);
      List<ReportListItemDto> reports =
          allReports.stream().filter(r -> !r.isReconciliationSummary()).toList();
      model.addAttribute(MODEL_REPORTS, reports);
    } catch (Exception ex) {
      log.warn("Failed to load reports fragment: {}", ex.getMessage());
      model.addAttribute(MODEL_REPORTS, List.of());
      model.addAttribute(
          "backendError",
          messageSource.getMessage("reports.error.service", null, LocaleContextHolder.getLocale()));
    }

    return "fragments/report-list :: reportList";
  }

  /**
   * GET /rapporter/{reportId}/download — downloads a single report as a zip file. Logs the download
   * to the audit log (ADR-0022).
   */
  @GetMapping("/{reportId}/download")
  public ResponseEntity<byte[]> downloadReport(@PathVariable UUID reportId, HttpSession session) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return ResponseEntity.status(401).build();
    }

    log.info(
        "Report download: creditor={}, reportId={}, user=portal-session", actingCreditor, reportId);

    try {
      byte[] content = reportingServiceClient.downloadReport(reportId);
      if (content == null) {
        return ResponseEntity.notFound().build();
      }

      String filename = "report-" + reportId + ".zip";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.parseMediaType("application/zip"));
      headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
      headers.setContentLength(content.length);

      return ResponseEntity.ok().headers(headers).body(content);
    } catch (Exception ex) {
      log.error("Report download failed for reportId={}: {}", reportId, ex.getMessage());
      return ResponseEntity.internalServerError().build();
    }
  }

  private void addRepresentedCreditorIfPresent(Model model, HttpSession session) {
    UUID representedCreditor = portalSessionService.getRepresentedCreditor(session);
    if (representedCreditor != null) {
      model.addAttribute("representedCreditorOrgId", representedCreditor);
    }
  }
}
