package dk.ufst.opendebt.caseworker.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.ufst.opendebt.caseworker.client.CaseServiceClient;
import dk.ufst.opendebt.caseworker.client.DebtServiceClient;
import dk.ufst.opendebt.caseworker.client.PersonRegistryClient;
import dk.ufst.opendebt.caseworker.client.RestPage;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;
import dk.ufst.opendebt.common.dto.CaseDto;
import dk.ufst.opendebt.common.dto.CaseJournalEntryDto;
import dk.ufst.opendebt.common.dto.CaseJournalNoteDto;
import dk.ufst.opendebt.common.dto.CasePartyDto;
import dk.ufst.opendebt.common.dto.CollectionMeasureDto;
import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.dto.ObjectionDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Displays case detail with tabs for overview, parties, debts, posteringslog, events, collection
 * measures, objections, and journal.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CaseDetailController {

  private final CaseServiceClient caseServiceClient;
  private final DebtServiceClient debtServiceClient;
  private final PersonRegistryClient personRegistryClient;
  private final CaseworkerSessionService sessionService;
  private final MessageSource messageSource;

  @GetMapping("/cases/{caseId}")
  public String caseDetail(@PathVariable UUID caseId, HttpSession session, Model model) {

    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null) {
      return "redirect:/demo-login";
    }

    model.addAttribute("caseworker", caseworker);
    model.addAttribute("currentPage", "cases");

    try {
      CaseDto caseDto = caseServiceClient.getCase(caseId);
      model.addAttribute("caseDto", caseDto);

      // Resolve debtor display name (backward-compatible via deprecated field)
      model.addAttribute("debtorDisplay", resolveDebtorDisplay(caseDto));

      // Load debts for this case
      if (caseDto.getDebtIds() != null && !caseDto.getDebtIds().isEmpty()) {
        RestPage<DebtDto> debts = debtServiceClient.listDebtsByIds(caseDto.getDebtIds());
        model.addAttribute("debts", debts.getContent());
      } else {
        model.addAttribute("debts", List.of());
      }

      // Load case parties
      loadParties(caseId, model);

      // Load collection measures
      loadMeasures(caseId, model);

      // Load objections
      loadObjections(caseId, model);

      // Load journal entries and notes
      loadJournal(caseId, model);

    } catch (Exception ex) {
      log.error("Failed to load case {}: {}", caseId, ex.getMessage());
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "case.detail.error.backend",
              null,
              "Failed to load case details. Please try again later.",
              LocaleContextHolder.getLocale()));
    }

    return "cases/detail";
  }

  private String resolveDebtorDisplay(CaseDto caseDto) {
    if (caseDto.getDebtorId() == null) return "\u2014";
    try {
      UUID debtorPersonId = UUID.fromString(caseDto.getDebtorId());
      String displayName = personRegistryClient.getDisplayName(debtorPersonId);
      return isResolvedDisplayName(displayName) ? displayName : caseDto.getDebtorId();
    } catch (IllegalArgumentException ex) {
      return caseDto.getDebtorId();
    }
  }

  private void loadParties(UUID caseId, Model model) {
    try {
      List<CasePartyDto> parties = caseServiceClient.getParties(caseId);
      model.addAttribute("parties", parties);
      model.addAttribute("partyDisplayNames", resolvePartyDisplayNames(caseId, parties));
    } catch (Exception ex) {
      log.warn("Failed to load parties for case {}: {}", caseId, ex.getMessage());
      model.addAttribute("parties", List.of());
      model.addAttribute("partyDisplayNames", Map.of());
    }
  }

  private Map<UUID, String> resolvePartyDisplayNames(UUID caseId, List<CasePartyDto> parties) {
    if (parties == null || parties.isEmpty()) {
      return Map.of();
    }

    Map<UUID, String> displayNames = new LinkedHashMap<>();
    for (CasePartyDto party : parties) {
      if (party == null
          || party.getPersonId() == null
          || displayNames.containsKey(party.getPersonId())) {
        continue;
      }

      try {
        String displayName = personRegistryClient.getDisplayName(party.getPersonId());
        displayNames.put(
            party.getPersonId(),
            isResolvedDisplayName(displayName) ? displayName : party.getPersonId().toString());
      } catch (Exception ex) {
        log.warn(
            "Failed to resolve display name for case party {} on case {}: {}",
            party.getPersonId(),
            caseId,
            ex.getMessage());
        displayNames.put(party.getPersonId(), party.getPersonId().toString());
      }
    }

    return displayNames;
  }

  private boolean isResolvedDisplayName(String displayName) {
    return displayName != null && !displayName.isBlank() && !"\u2014".equals(displayName);
  }

  private void loadMeasures(UUID caseId, Model model) {
    try {
      List<CollectionMeasureDto> measures = caseServiceClient.getMeasures(caseId);
      model.addAttribute("measures", measures);
    } catch (Exception ex) {
      log.warn("Failed to load measures for case {}: {}", caseId, ex.getMessage());
      model.addAttribute("measures", List.of());
    }
  }

  private void loadObjections(UUID caseId, Model model) {
    try {
      List<ObjectionDto> objections = caseServiceClient.getObjections(caseId);
      model.addAttribute("objections", objections);
    } catch (Exception ex) {
      log.warn("Failed to load objections for case {}: {}", caseId, ex.getMessage());
      model.addAttribute("objections", List.of());
    }
  }

  private void loadJournal(UUID caseId, Model model) {
    try {
      List<CaseJournalEntryDto> journalEntries = caseServiceClient.getJournalEntries(caseId);
      model.addAttribute("journalEntries", journalEntries);
    } catch (Exception ex) {
      log.warn("Failed to load journal entries for case {}: {}", caseId, ex.getMessage());
      model.addAttribute("journalEntries", List.of());
    }
    try {
      List<CaseJournalNoteDto> journalNotes = caseServiceClient.getJournalNotes(caseId);
      model.addAttribute("journalNotes", journalNotes);
    } catch (Exception ex) {
      log.warn("Failed to load journal notes for case {}: {}", caseId, ex.getMessage());
      model.addAttribute("journalNotes", List.of());
    }
  }
}
