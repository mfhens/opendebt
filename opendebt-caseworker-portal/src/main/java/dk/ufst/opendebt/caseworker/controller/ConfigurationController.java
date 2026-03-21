package dk.ufst.opendebt.caseworker.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dk.ufst.opendebt.caseworker.client.ConfigServiceClient;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.dto.config.ConfigEntryPortalDto;
import dk.ufst.opendebt.caseworker.dto.config.CreateConfigPortalRequest;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Portal view controller for managing versioned business configuration values. Implements petition
 * 047 §FR-1–FR-8: displays the config admin UI for ADMIN and CONFIGURATION_MANAGER roles.
 * CASEWORKER roles get a read-only view.
 */
@Slf4j
@Controller
@RequestMapping("/konfiguration")
@RequiredArgsConstructor
public class ConfigurationController {

  private static final String ROLE_ADMIN = "ADMIN";
  private static final String ROLE_CONFIG_MGR = "CONFIGURATION_MANAGER";

  private final ConfigServiceClient configServiceClient;
  private final CaseworkerSessionService sessionService;

  /** GET /konfiguration — list all config entries grouped by key. */
  @GetMapping
  public String list(HttpSession session, Model model) {
    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null) {
      return "redirect:/demo-login";
    }

    try {
      Map<String, List<ConfigEntryPortalDto>> grouped = configServiceClient.listAllGrouped();
      model.addAttribute("grouped", grouped);
    } catch (Exception e) {
      log.warn("Failed to load config list: {}", e.getMessage());
      model.addAttribute("backendError", "Konfigurationstjenesten er ikke tilgængelig.");
      model.addAttribute("grouped", Map.of());
    }

    model.addAttribute("caseworker", caseworker);
    model.addAttribute("currentPage", "konfiguration");
    model.addAttribute("canEdit", isEditor(caseworker));
    return "config/list";
  }

  /** GET /konfiguration/{key} — version history for a single config key. */
  @GetMapping("/{key}")
  public String detail(@PathVariable String key, HttpSession session, Model model) {
    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null) {
      return "redirect:/demo-login";
    }

    try {
      List<ConfigEntryPortalDto> history = configServiceClient.getHistory(key);
      model.addAttribute("history", history);
      model.addAttribute("configKey", key);
    } catch (Exception e) {
      log.warn("Failed to load config history for key={}: {}", key, e.getMessage());
      model.addAttribute("backendError", "Kunne ikke indlæse konfigurationshistorik.");
      model.addAttribute("history", List.of());
    }

    model.addAttribute("caseworker", caseworker);
    model.addAttribute("currentPage", "konfiguration");
    model.addAttribute("canEdit", isEditor(caseworker));
    return "config/detail";
  }

  /** GET /konfiguration/{key}/preview?nbRate=&validFrom= — preview derived rates (HTMX). */
  @GetMapping("/{key}/preview")
  public String previewDerived(
      @PathVariable String key,
      @RequestParam BigDecimal nbRate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom,
      HttpSession session,
      Model model) {
    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null || !isEditor(caseworker)) {
      return "redirect:/demo-login";
    }

    try {
      List<ConfigEntryPortalDto> preview =
          configServiceClient.previewDerived(key, nbRate, validFrom);
      model.addAttribute("preview", preview);
      model.addAttribute("nbRate", nbRate);
      model.addAttribute("validFrom", validFrom);
    } catch (Exception e) {
      log.warn("Failed to preview derived rates: {}", e.getMessage());
      model.addAttribute("previewError", "Forhåndsvisning mislykkedes: " + e.getMessage());
      model.addAttribute("preview", List.of());
    }

    return "config/fragments/derived-preview :: derivedPreview";
  }

  /** POST /konfiguration — create a new config version. */
  @PostMapping
  public String create(
      @ModelAttribute CreateConfigPortalRequest request,
      HttpSession session,
      RedirectAttributes redirectAttributes) {
    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null || !isEditor(caseworker)) {
      return "redirect:/demo-login";
    }

    try {
      configServiceClient.createEntry(request);
      redirectAttributes.addFlashAttribute(
          "successMessage", "Konfigurationsværdi oprettet og afventer godkendelse.");
      log.info("Config entry created: key={} by={}", request.getConfigKey(), caseworker.getId());
    } catch (Exception e) {
      log.warn("Failed to create config entry: {}", e.getMessage());
      redirectAttributes.addFlashAttribute(
          "errorMessage", "Fejl ved oprettelse af konfigurationsværdi: " + e.getMessage());
    }

    return "redirect:/konfiguration";
  }

  /** PUT /konfiguration/{id}/approve — approve a PENDING_REVIEW entry. */
  @PostMapping("/{id}/approve")
  public String approve(
      @PathVariable UUID id, HttpSession session, RedirectAttributes redirectAttributes) {
    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null || !isEditor(caseworker)) {
      return "redirect:/demo-login";
    }

    try {
      configServiceClient.approveEntry(id);
      redirectAttributes.addFlashAttribute("successMessage", "Konfigurationsværdi godkendt.");
      log.info("Config entry approved: id={} by={}", id, caseworker.getId());
    } catch (Exception e) {
      log.warn("Failed to approve config entry id={}: {}", id, e.getMessage());
      redirectAttributes.addFlashAttribute(
          "errorMessage", "Fejl ved godkendelse: " + e.getMessage());
    }

    return "redirect:/konfiguration";
  }

  /** POST /konfiguration/{id}/reject — reject a PENDING_REVIEW entry. */
  @PostMapping("/{id}/reject")
  public String reject(
      @PathVariable UUID id, HttpSession session, RedirectAttributes redirectAttributes) {
    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null || !isEditor(caseworker)) {
      return "redirect:/demo-login";
    }

    try {
      configServiceClient.rejectEntry(id);
      redirectAttributes.addFlashAttribute("successMessage", "Konfigurationsværdi afvist.");
      log.info("Config entry rejected: id={} by={}", id, caseworker.getId());
    } catch (Exception e) {
      log.warn("Failed to reject config entry id={}: {}", id, e.getMessage());
      redirectAttributes.addFlashAttribute("errorMessage", "Fejl ved afvisning: " + e.getMessage());
    }

    return "redirect:/konfiguration";
  }

  /** POST /konfiguration/{id}/delete — delete a future entry. */
  @PostMapping("/{id}/delete")
  public String delete(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "") String returnKey,
      HttpSession session,
      RedirectAttributes redirectAttributes) {
    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null || !isEditor(caseworker)) {
      return "redirect:/demo-login";
    }

    try {
      configServiceClient.deleteEntry(id);
      redirectAttributes.addFlashAttribute(
          "successMessage", "Fremtidig konfigurationsværdi slettet.");
      log.info("Config entry deleted: id={} by={}", id, caseworker.getId());
    } catch (Exception e) {
      log.warn("Failed to delete config entry id={}: {}", id, e.getMessage());
      redirectAttributes.addFlashAttribute("errorMessage", "Fejl ved sletning: " + e.getMessage());
    }

    return returnKey.isEmpty() ? "redirect:/konfiguration" : "redirect:/konfiguration/" + returnKey;
  }

  private boolean isEditor(CaseworkerIdentity caseworker) {
    String role = caseworker.getRole();
    return ROLE_ADMIN.equals(role) || ROLE_CONFIG_MGR.equals(role);
  }
}
