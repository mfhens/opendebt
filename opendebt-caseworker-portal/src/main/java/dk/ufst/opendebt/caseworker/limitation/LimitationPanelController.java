package dk.ufst.opendebt.caseworker.limitation;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class LimitationPanelController {

  private final DebtServiceLimitationClient debtServiceLimitationClient;
  private final CaseworkerSessionService caseworkerSessionService;

  @GetMapping("/cases/{caseId}/debts/{fordringId}/limitation-panel")
  public String limitationPanel(
      @PathVariable UUID caseId, @PathVariable UUID fordringId, HttpSession session, Model model) {
    CaseworkerIdentity caseworker = caseworkerSessionService.getCurrentCaseworker(session);
    if (caseworker == null) {
      return "redirect:/demo-login";
    }
    LimitationPanelData status = debtServiceLimitationClient.getLimitationStatus(fordringId);
    List<UUID> members =
        status.getKompleksId() == null
            ? status.getMemberFordringIds()
            : debtServiceLimitationClient
                .getClaimComplexMembers(status.getKompleksId())
                .getMemberFordringIds();
    model.addAttribute("caseId", caseId);
    model.addAttribute("fordringId", fordringId);
    model.addAttribute("limitation", status);
    model.addAttribute("memberFordringIds", members == null ? List.of() : members);
    model.addAttribute("statusLabel", statusLabel(status.getStatus()));
    model.addAttribute(
        "yesNoUdskydelse", Boolean.TRUE.equals(status.getIsInUdskydelse()) ? "Ja" : "Nej");
    model.addAttribute(
        "canRegisterObjection", hasWriteAccess(caseworker) && "ACTIVE".equals(status.getStatus()));
    model.addAttribute(
        "showEvaluationForm",
        hasWriteAccess(caseworker) && "INDSIGELSE_PENDING".equals(status.getStatus()));
    model.addAttribute("readOnly", !hasWriteAccess(caseworker));
    return "limitation/limitation-panel";
  }

  private boolean hasWriteAccess(CaseworkerIdentity caseworker) {
    return caseworker != null
        && ("CASEWORKER".equals(caseworker.getRole()) || "ADMIN".equals(caseworker.getRole()));
  }

  private String statusLabel(String status) {
    if ("FORAELDET".equals(status)) {
      return "Forældet";
    }
    if ("INDSIGELSE_PENDING".equals(status)) {
      return "Afventer indsigelse";
    }
    return "Aktiv";
  }
}
