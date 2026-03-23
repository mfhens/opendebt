package dk.ufst.opendebt.citizen.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.ufst.opendebt.citizen.client.CaseServiceClient;
import dk.ufst.opendebt.common.dto.CaseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Renders the case detail page for citizens. Provides the page shell with case reference in the
 * heading; timeline content is loaded lazily via HTMX by {@link CitizenTimelineController}. Ref:
 * petition050 specs §4.6.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('CITIZEN')")
public class CaseDetailController {

  private final CaseServiceClient caseServiceClient;

  /** Renders the case detail page. The Tidslinje tab loads its content lazily via HTMX. */
  @GetMapping("/cases/{caseId}")
  public String showCaseDetail(@PathVariable UUID caseId, Model model) {
    model.addAttribute("caseId", caseId);

    try {
      CaseDto caseDto = caseServiceClient.getCase(caseId);
      String caseReference =
          (caseDto != null && caseDto.getCaseNumber() != null)
              ? caseDto.getCaseNumber()
              : caseId.toString();
      model.addAttribute("caseReference", caseReference);
    } catch (Exception e) {
      log.warn("Failed to load case metadata for caseId={}: {}", caseId, e.getMessage());
      model.addAttribute("caseReference", caseId.toString());
      model.addAttribute("serviceError", "Der opstod en fejl ved indlæsning af sagsoplysninger.");
    }

    return "cases/detail";
  }
}
