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

import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.dto.FordringFormDto;
import dk.ufst.opendebt.creditor.dto.PortalDebtDto;
import dk.ufst.opendebt.creditor.mapper.FordringMapper;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Controller for manual fordring (debt claim) submission. */
@Slf4j
@Controller
@RequestMapping("/fordring")
@RequiredArgsConstructor
public class FordringController {

  private final DebtServiceClient debtServiceClient;
  private final FordringMapper fordringMapper;
  private final MessageSource messageSource;
  private final PortalSessionService portalSessionService;

  /** GET /fordring/ny – displays the new fordring form. */
  @GetMapping("/ny")
  public String showForm(Model model, HttpSession session) {
    if (portalSessionService.resolveActingCreditor(null, session) == null) {
      return "redirect:/demo-login";
    }
    if (!model.containsAttribute("fordringForm")) {
      model.addAttribute("fordringForm", new FordringFormDto());
    }
    addActingCreditorToModel(model, session);
    return "fordring-ny";
  }

  /** POST /fordring/ny – validates and submits the new fordring. */
  @PostMapping("/ny")
  public String submitForm(
      @Valid @ModelAttribute("fordringForm") FordringFormDto form,
      BindingResult bindingResult,
      Model model,
      HttpSession session,
      RedirectAttributes redirectAttributes) {

    UUID actingCreditorOrgId = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditorOrgId == null) {
      return "redirect:/demo-login";
    }

    if (bindingResult.hasErrors()) {
      addActingCreditorToModel(model, session);
      return "fordring-ny";
    }

    try {
      PortalDebtDto request = fordringMapper.toDebtRequest(form, actingCreditorOrgId);
      debtServiceClient.createDebt(request);

      redirectAttributes.addFlashAttribute(
          "successMessage",
          messageSource.getMessage(
              "controller.fordring.submitted", null, LocaleContextHolder.getLocale()));
      return "redirect:/fordringer";
    } catch (Exception ex) {
      log.error("Fejl ved indsendelse af fordring: {}", ex.getMessage(), ex);
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "controller.fordring.submit.error", null, LocaleContextHolder.getLocale()));
      addActingCreditorToModel(model, session);
      return "fordring-ny";
    }
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
