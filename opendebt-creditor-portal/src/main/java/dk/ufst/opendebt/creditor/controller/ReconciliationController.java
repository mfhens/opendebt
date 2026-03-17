package dk.ufst.opendebt.creditor.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dk.ufst.opendebt.creditor.client.ReconciliationServiceClient;
import dk.ufst.opendebt.creditor.dto.ReconciliationBasisDto;
import dk.ufst.opendebt.creditor.dto.ReconciliationDetailDto;
import dk.ufst.opendebt.creditor.dto.ReconciliationListItemDto;
import dk.ufst.opendebt.creditor.dto.ReconciliationResponseDto;
import dk.ufst.opendebt.creditor.dto.ReconciliationResponseFormDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Controller for reconciliation list, detail, and response submission (petition 036). */
@Slf4j
@Controller
@RequestMapping("/afstemning")
@RequiredArgsConstructor
public class ReconciliationController {

  private final ReconciliationServiceClient reconciliationServiceClient;
  private final MessageSource messageSource;
  private final PortalSessionService portalSessionService;

  /** GET /afstemning — displays the reconciliation list with optional filters. */
  @GetMapping
  public String list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate periodEndFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate periodEndTo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate reconciliationStartFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate reconciliationStartTo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate reconciliationEndFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate reconciliationEndTo,
      Model model,
      HttpSession session) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    List<ReconciliationListItemDto> reconciliations;
    try {
      reconciliations =
          reconciliationServiceClient.listReconciliations(
              actingCreditor,
              status,
              periodEndFrom,
              periodEndTo,
              reconciliationStartFrom,
              reconciliationStartTo,
              reconciliationEndFrom,
              reconciliationEndTo);
    } catch (Exception ex) {
      log.error("Failed to load reconciliations: {}", ex.getMessage());
      reconciliations = List.of();
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "reconciliation.error.service", null, LocaleContextHolder.getLocale()));
    }

    model.addAttribute("reconciliations", reconciliations);
    model.addAttribute("currentPage", "reconciliation");
    model.addAttribute("filterStatus", status);
    model.addAttribute("filterPeriodEndFrom", periodEndFrom);
    model.addAttribute("filterPeriodEndTo", periodEndTo);
    model.addAttribute("filterReconciliationStartFrom", reconciliationStartFrom);
    model.addAttribute("filterReconciliationStartTo", reconciliationStartTo);
    model.addAttribute("filterReconciliationEndFrom", reconciliationEndFrom);
    model.addAttribute("filterReconciliationEndTo", reconciliationEndTo);
    addActingCreditorToModel(model, session);
    return "reconciliation/list";
  }

  /** GET /afstemning/{id} — displays the reconciliation detail view. */
  @GetMapping("/{id}")
  public String detail(@PathVariable UUID id, Model model, HttpSession session) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    ReconciliationDetailDto detail;
    try {
      detail = reconciliationServiceClient.getReconciliationDetail(id);
    } catch (Exception ex) {
      log.error("Failed to load reconciliation detail: {}", ex.getMessage());
      detail = null;
    }

    if (detail == null) {
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "reconciliation.error.notfound", null, LocaleContextHolder.getLocale()));
      model.addAttribute("currentPage", "reconciliation");
      addActingCreditorToModel(model, session);
      return "reconciliation/detail";
    }

    // For ACTIVE reconciliations, also load basis data
    if ("ACTIVE".equalsIgnoreCase(detail.getStatus())) {
      ReconciliationBasisDto basis;
      try {
        basis = reconciliationServiceClient.getReconciliationBasis(id);
      } catch (Exception ex) {
        log.warn("Failed to load reconciliation basis: {}", ex.getMessage());
        basis = ReconciliationBasisDto.builder().build();
      }
      detail.setBasis(basis);
      model.addAttribute("basisChecksum", computeBasisChecksum(basis));
    }

    model.addAttribute("reconciliation", detail);
    model.addAttribute("currentPage", "reconciliation");
    if (!model.containsAttribute("responseForm")) {
      model.addAttribute("responseForm", new ReconciliationResponseFormDto());
    }
    addActingCreditorToModel(model, session);
    return "reconciliation/detail";
  }

  /** POST /afstemning/{id}/confirm — shows confirmation step before final submission. */
  @PostMapping("/{id}/confirm")
  public String confirmResponse(
      @PathVariable UUID id,
      @Valid @ModelAttribute("responseForm") ReconciliationResponseFormDto form,
      BindingResult bindingResult,
      Model model,
      HttpSession session) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    // Custom validation: explained + unexplained == total
    validateTotalDifference(form, bindingResult);

    if (bindingResult.hasErrors()) {
      return reloadDetailWithErrors(id, model, session);
    }

    // Load the reconciliation for display in confirmation step
    ReconciliationDetailDto detail = reconciliationServiceClient.getReconciliationDetail(id);
    if (detail == null || !"ACTIVE".equalsIgnoreCase(detail.getStatus())) {
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "reconciliation.error.notactive", null, LocaleContextHolder.getLocale()));
      model.addAttribute("currentPage", "reconciliation");
      addActingCreditorToModel(model, session);
      return "reconciliation/detail";
    }

    ReconciliationBasisDto basis = reconciliationServiceClient.getReconciliationBasis(id);
    detail.setBasis(basis);

    // Tamper check: verify basis checksum
    String expectedChecksum = computeBasisChecksum(basis);
    if (form.getBasisChecksum() != null && !form.getBasisChecksum().equals(expectedChecksum)) {
      log.warn("Basis data tamper detected for reconciliation: {}", id);
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "reconciliation.error.tamper", null, LocaleContextHolder.getLocale()));
      model.addAttribute("reconciliation", detail);
      model.addAttribute("currentPage", "reconciliation");
      addActingCreditorToModel(model, session);
      return "reconciliation/detail";
    }

    model.addAttribute("reconciliation", detail);
    model.addAttribute("responseForm", form);
    model.addAttribute("currentPage", "reconciliation");
    model.addAttribute("showConfirmation", true);
    addActingCreditorToModel(model, session);
    return "reconciliation/detail";
  }

  /** POST /afstemning/{id}/response — submits the confirmed reconciliation response. */
  @PostMapping("/{id}/response")
  public String submitResponse(
      @PathVariable UUID id,
      @Valid @ModelAttribute("responseForm") ReconciliationResponseFormDto form,
      BindingResult bindingResult,
      Model model,
      HttpSession session,
      RedirectAttributes redirectAttributes) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    // Re-validate
    validateTotalDifference(form, bindingResult);

    if (bindingResult.hasErrors()) {
      return reloadDetailWithErrors(id, model, session);
    }

    // Tamper check: re-verify basis from backend
    ReconciliationBasisDto currentBasis;
    try {
      currentBasis = reconciliationServiceClient.getReconciliationBasis(id);
    } catch (Exception ex) {
      log.error("Failed to verify basis data: {}", ex.getMessage());
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "reconciliation.error.service", null, LocaleContextHolder.getLocale()));
      return reloadDetailWithErrors(id, model, session);
    }

    String expectedChecksum = computeBasisChecksum(currentBasis);
    if (form.getBasisChecksum() != null && !form.getBasisChecksum().equals(expectedChecksum)) {
      log.warn("Basis data tamper detected during submission for reconciliation: {}", id);
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "reconciliation.error.tamper", null, LocaleContextHolder.getLocale()));
      return reloadDetailWithErrors(id, model, session);
    }

    // Build the response DTO and submit
    ReconciliationResponseDto responseDto =
        ReconciliationResponseDto.builder()
            .explainedDifference(form.getExplainedDifference())
            .unexplainedDifference(form.getUnexplainedDifference())
            .totalDifference(form.getTotalDifference())
            .build();

    try {
      reconciliationServiceClient.submitReconciliationResponse(id, responseDto);
      redirectAttributes.addFlashAttribute(
          "successMessage",
          messageSource.getMessage(
              "reconciliation.submit.success", null, LocaleContextHolder.getLocale()));
      return "redirect:/afstemning/" + id;
    } catch (Exception ex) {
      log.error("Failed to submit reconciliation response: {}", ex.getMessage());
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "reconciliation.submit.error", null, LocaleContextHolder.getLocale()));
      return reloadDetailWithErrors(id, model, session);
    }
  }

  /** Validates that explainedDifference + unexplainedDifference == totalDifference. */
  private void validateTotalDifference(
      ReconciliationResponseFormDto form, BindingResult bindingResult) {
    if (form.getExplainedDifference() != null
        && form.getUnexplainedDifference() != null
        && form.getTotalDifference() != null) {
      BigDecimal sum = form.getExplainedDifference().add(form.getUnexplainedDifference());
      if (sum.compareTo(form.getTotalDifference()) != 0) {
        bindingResult.rejectValue(
            "totalDifference",
            "reconciliation.validation.total.mismatch",
            messageSource.getMessage(
                "reconciliation.validation.total.mismatch", null, LocaleContextHolder.getLocale()));
      }
    }
  }

  /** Computes a SHA-256 checksum of the basis data for tamper protection. */
  String computeBasisChecksum(ReconciliationBasisDto basis) {
    if (basis == null) {
      return "";
    }
    String data =
        String.join(
            "|",
            basis.getInfluxAmount().toPlainString(),
            basis.getRecallAmount().toPlainString(),
            basis.getWriteUpAmount().toPlainString(),
            basis.getWriteDownAmount().toPlainString());
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      log.error("SHA-256 not available", ex);
      return "";
    }
  }

  /** Reloads the detail page after validation errors. */
  private String reloadDetailWithErrors(UUID id, Model model, HttpSession session) {
    ReconciliationDetailDto detail = reconciliationServiceClient.getReconciliationDetail(id);
    if (detail != null && "ACTIVE".equalsIgnoreCase(detail.getStatus())) {
      ReconciliationBasisDto basis = reconciliationServiceClient.getReconciliationBasis(id);
      detail.setBasis(basis);
      model.addAttribute("basisChecksum", computeBasisChecksum(basis));
    }
    model.addAttribute("reconciliation", detail);
    model.addAttribute("currentPage", "reconciliation");
    addActingCreditorToModel(model, session);
    return "reconciliation/detail";
  }

  private void addActingCreditorToModel(Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    UUID representedCreditor = portalSessionService.getRepresentedCreditor(session);
    model.addAttribute("actingCreditorOrgId", actingCreditor);
    if (representedCreditor != null) {
      model.addAttribute("representedCreditorOrgId", representedCreditor);
    }
  }
}
