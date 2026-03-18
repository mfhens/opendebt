package dk.ufst.opendebt.caseworker.controller;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import dk.ufst.opendebt.caseworker.client.PaymentServiceClient;
import dk.ufst.opendebt.caseworker.client.RestPage;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.dto.PortalLedgerEntryDto;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Exports posteringslog data as CSV for download. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CsvExportController {

  private static final String CSV_HEADER =
      "Vaerdidag;Bogfoeringsdag;Konto;KontoNavn;Type;Beloeb;Kategori;Reference;Beskrivelse;FordringId";

  private final PaymentServiceClient paymentServiceClient;
  private final CaseworkerSessionService sessionService;

  @GetMapping("/cases/{caseId}/debts/{debtId}/posteringslog/csv")
  public void exportCsv(
      @PathVariable UUID caseId,
      @PathVariable UUID debtId,
      @RequestParam(name = "category", required = false) String category,
      @RequestParam(name = "fromDate", required = false) String fromDate,
      @RequestParam(name = "toDate", required = false) String toDate,
      HttpSession session,
      HttpServletResponse response)
      throws Exception {

    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null) {
      response.sendRedirect("../../..");
      return;
    }

    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader(
        "Content-Disposition", "attachment; filename=\"posteringslog-" + debtId + ".csv\"");

    try (PrintWriter writer =
        new PrintWriter(
            new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
      // BOM for Excel UTF-8 compatibility
      writer.print('\uFEFF');
      writer.println(CSV_HEADER);

      RestPage<PortalLedgerEntryDto> ledger =
          paymentServiceClient.getLedgerByDebt(debtId, category, fromDate, toDate, 0, 10000);

      for (PortalLedgerEntryDto entry : ledger.getContent()) {
        writer.printf(
            "%s;%s;%s;%s;%s;%s;%s;%s;%s;%s%n",
            entry.getEffectiveDate() != null ? entry.getEffectiveDate() : "",
            entry.getPostingDate() != null ? entry.getPostingDate() : "",
            entry.getAccountCode() != null ? entry.getAccountCode() : "",
            csvEscape(entry.getAccountName()),
            entry.getEntryType() != null ? entry.getEntryType() : "",
            entry.getAmount() != null ? entry.getAmount().toPlainString() : "",
            entry.getEntryCategory() != null ? entry.getEntryCategory() : "",
            csvEscape(entry.getReference()),
            csvEscape(entry.getDescription()),
            entry.getDebtId() != null ? entry.getDebtId() : "");
      }
    }
  }

  private String csvEscape(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
