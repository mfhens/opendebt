package dk.ufst.opendebt.creditor.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.dto.ContactEmailUpdateDto;
import dk.ufst.opendebt.creditor.dto.CreditorAgreementDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Controller for the creditor portal settings page. */
@Slf4j
@Controller
@RequestMapping("/indstillinger")
@RequiredArgsConstructor
public class SettingsController {

  private final CreditorServiceClient creditorServiceClient;
  private final MessageSource messageSource;
  private final PortalSessionService portalSessionService;

  /** GET /indstillinger — displays the settings page with agreement config and contact email. */
  @GetMapping
  public String showSettings(Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    model.addAttribute("currentPage", "settings");
    loadAgreement(actingCreditor, model);

    if (!model.containsAttribute("contactEmailForm")) {
      CreditorAgreementDto agreement = (CreditorAgreementDto) model.getAttribute("agreement");
      String existingEmail = agreement != null ? agreement.getContactEmail() : "";
      model.addAttribute(
          "contactEmailForm",
          ContactEmailUpdateDto.builder()
              .contactEmail(existingEmail != null ? existingEmail : "")
              .build());
    }

    return "indstillinger";
  }

  /** POST /indstillinger — updates the creditor contact email. */
  @PostMapping
  public String updateContactEmail(
      @Valid @ModelAttribute("contactEmailForm") ContactEmailUpdateDto form,
      BindingResult bindingResult,
      Model model,
      HttpSession session,
      RedirectAttributes redirectAttributes) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    if (bindingResult.hasErrors()) {
      model.addAttribute("currentPage", "settings");
      loadAgreement(actingCreditor, model);
      return "indstillinger";
    }

    try {
      creditorServiceClient.updateContactEmail(actingCreditor, form);
      redirectAttributes.addFlashAttribute(
          "successMessage",
          messageSource.getMessage(
              "settings.email.updated", null, LocaleContextHolder.getLocale()));
    } catch (Exception ex) {
      log.error("Failed to update contact email: {}", ex.getMessage());
      redirectAttributes.addFlashAttribute(
          "errorMessage",
          messageSource.getMessage(
              "settings.email.update.error", null, LocaleContextHolder.getLocale()));
    }

    return "redirect:/indstillinger";
  }

  private void loadAgreement(UUID actingCreditor, Model model) {
    try {
      CreditorAgreementDto agreement = creditorServiceClient.getCreditorAgreement(actingCreditor);
      model.addAttribute("agreement", agreement);
    } catch (Exception ex) {
      log.warn("Failed to load creditor agreement: {}", ex.getMessage());
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "settings.backend.unavailable", null, LocaleContextHolder.getLocale()));
    }
  }
}
