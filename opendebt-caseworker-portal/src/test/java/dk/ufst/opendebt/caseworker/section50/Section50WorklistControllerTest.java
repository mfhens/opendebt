package dk.ufst.opendebt.caseworker.section50;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;

@ExtendWith(MockitoExtension.class)
class Section50WorklistControllerTest {

  @Mock private Section50WorklistClient section50WorklistClient;
  @Mock private CaseworkerSessionService caseworkerSessionService;
  @Mock private MessageSource messageSource;
  @InjectMocks private Section50WorklistController controller;

  private final MockHttpSession session = new MockHttpSession();
  private final Model model = new ExtendedModelMap();
  private static final UUID DEBTOR_ID = UUID.randomUUID();
  private static final UUID WORKLIST_ID = UUID.randomUUID();

  @Test
  void worklist_whenNoSession_redirectsToLogin() {
    when(caseworkerSessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view = controller.worklist(DEBTOR_ID, WORKLIST_ID, session, model);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void worklist_success_populatesModel() {
    when(caseworkerSessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    when(section50WorklistClient.getWorklist(DEBTOR_ID, WORKLIST_ID)).thenReturn(worklist());

    String view = controller.worklist(DEBTOR_ID, WORKLIST_ID, session, model);

    assertThat(view).isEqualTo("section50/worklist");
    assertThat(model.asMap()).containsKey("worklist");
    assertThat(model.asMap()).containsEntry("debtorId", DEBTOR_ID);
    assertThat(model.asMap()).containsEntry("worklistId", WORKLIST_ID);
    assertThat(model.asMap()).containsEntry("showExpeditedForm", false);
    assertThat(model.asMap()).containsEntry("showModregningForm", true);
  }

  @Test
  void worklist_voluntaryPaymentSurplus_showsExpeditedOnly() {
    when(caseworkerSessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    when(section50WorklistClient.getWorklist(DEBTOR_ID, WORKLIST_ID))
        .thenReturn(worklist("VOLUNTARY_PAYMENT_SURPLUS"));

    String view = controller.worklist(DEBTOR_ID, WORKLIST_ID, session, model);

    assertThat(view).isEqualTo("section50/worklist");
    assertThat(model.asMap()).containsEntry("showExpeditedForm", true);
    assertThat(model.asMap()).containsEntry("showModregningForm", false);
  }

  @Test
  void worklist_whenBackendFails_setsBackendError() {
    when(caseworkerSessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    when(section50WorklistClient.getWorklist(DEBTOR_ID, WORKLIST_ID))
        .thenThrow(new RuntimeException("timeout"));
    when(messageSource.getMessage(anyString(), any(), anyString(), any()))
        .thenReturn("Section 50 worklist unavailable");

    String view = controller.worklist(DEBTOR_ID, WORKLIST_ID, session, model);

    assertThat(view).isEqualTo("section50/worklist");
    assertThat(model.asMap()).containsKey("backendError");
  }

  @Test
  void applyOverride_withWriteAccess_callsClientAndRedirects() {
    when(caseworkerSessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    when(messageSource.getMessage(anyString(), any(), anyString(), any()))
        .thenReturn("Retskraftvurderingen er opdateret.");
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    Section50WorklistController.Section50OverrideForm form =
        new Section50WorklistController.Section50OverrideForm();
    form.setOverrideReason("Urgent court deadline on C-06022");
    form.setLegalBasis("Section 50 override");
    form.setSelectedClaimOrder("C-06022\nC-06021");
    form.setExpedited(Boolean.FALSE);

    String view =
        controller.applyOverride(DEBTOR_ID, WORKLIST_ID, form, session, redirectAttributes);

    assertThat(view)
        .isEqualTo("redirect:/debtors/" + DEBTOR_ID + "/retskraft-worklists/" + WORKLIST_ID);
    assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
  }

  @Test
  void recordModregningDecision_withReadOnlyRole_redirectsToLogin() {
    when(caseworkerSessionService.getCurrentCaseworker(session)).thenReturn(readOnlyCaseworker());
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    Section50WorklistController.Section50ModregningDecisionForm form =
        new Section50WorklistController.Section50ModregningDecisionForm();
    form.setModregningOutcome("NO_MODREGNING");
    form.setReason("Timing pressure before payout deadline");

    String view =
        controller.recordModregningDecision(
            DEBTOR_ID, WORKLIST_ID, form, session, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  private PortalSection50WorklistDto worklist() {
    return worklist("MODREGNING");
  }

  private PortalSection50WorklistDto worklist(String contextType) {
    return new PortalSection50WorklistDto(
        WORKLIST_ID,
        DEBTOR_ID,
        "OVERRIDE",
        "Section 50 override path",
        contextType,
        BigDecimal.valueOf(500),
        Instant.parse("2026-05-27T08:15:00Z"),
        "C-06022",
        "Urgent court deadline on C-06022",
        "Section 50 subsection 5",
        "Timing pressure before payout deadline",
        "NO_MODREGNING",
        List.of(
            new PortalSection50WorklistEntryDto(
                1,
                "C-06022",
                "PRINCIPAL",
                "OTHER",
                false,
                false,
                true,
                "Override selected claim first",
                List.of("urgent", "deadline"),
                null,
                BigDecimal.valueOf(250))),
        new PortalSection50DecisionSnapshotDto(
            UUID.randomUUID(),
            WORKLIST_ID,
            "SECTION_50_OVERRIDE_PATH",
            "abc123hash",
            "C-06022",
            "Section 50 override path",
            UUID.randomUUID(),
            "CASEWORKER",
            Instant.parse("2026-05-27T08:15:00Z"),
            "Timing pressure before payout deadline",
            List.of("timing", "deadline")));
  }

  private CaseworkerIdentity caseworker() {
    return CaseworkerIdentity.builder().id("u1").name("Test").role("CASEWORKER").build();
  }

  private CaseworkerIdentity readOnlyCaseworker() {
    return CaseworkerIdentity.builder()
        .id("u2")
        .name("Read Only")
        .role("SENIOR_CASEWORKER")
        .build();
  }
}
