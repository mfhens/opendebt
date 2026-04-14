package dk.ufst.opendebt.caseworker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
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

@ExtendWith(MockitoExtension.class)
class CaseDetailControllerTest {

  @Mock private CaseServiceClient caseServiceClient;
  @Mock private DebtServiceClient debtServiceClient;
  @Mock private PersonRegistryClient personRegistryClient;
  @Mock private CaseworkerSessionService sessionService;
  @Mock private MessageSource messageSource;
  @InjectMocks private CaseDetailController controller;

  private final MockHttpSession session = new MockHttpSession();
  private final Model model = new ExtendedModelMap();
  private static final UUID CASE_ID = UUID.randomUUID();
  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final UUID PARTY_ID = UUID.randomUUID();

  @SuppressWarnings("unchecked")
  private Map<UUID, String> partyDisplayNames() {
    return (Map<UUID, String>) model.asMap().get("partyDisplayNames");
  }

  @Test
  void caseDetail_whenNoSession_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view = controller.caseDetail(CASE_ID, session, model);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void caseDetail_success_withDebts_populatesModel() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    CaseDto caseDto =
        CaseDto.builder()
            .id(CASE_ID)
            .debtorId(UUID.randomUUID().toString())
            .debtIds(List.of(DEBT_ID))
            .build();
    when(caseServiceClient.getCase(CASE_ID)).thenReturn(caseDto);
    when(personRegistryClient.getDisplayName(any(UUID.class))).thenReturn("Person-abc123");
    when(debtServiceClient.listDebtsByIds(any()))
        .thenReturn(new RestPage<>(List.of(DebtDto.builder().build()), 0, 20, 1, 1));
    when(caseServiceClient.getParties(CASE_ID))
        .thenReturn(List.of(CasePartyDto.builder().personId(PARTY_ID).build()));
    when(caseServiceClient.getMeasures(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getObjections(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getJournalEntries(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getJournalNotes(CASE_ID)).thenReturn(List.of());

    String view = controller.caseDetail(CASE_ID, session, model);

    assertThat(view).isEqualTo("cases/detail");
    assertThat(model.asMap()).containsKey("caseDto");
    assertThat(model.asMap()).containsKey("debts");
    assertThat(model.asMap()).containsKey("debtorDisplay");
    assertThat(model.asMap()).containsKey("partyDisplayNames");
    assertThat(partyDisplayNames()).containsEntry(PARTY_ID, "Person-abc123");
  }

  @Test
  void caseDetail_success_withNoDebtIds_setsEmptyDebtList() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    CaseDto caseDto = CaseDto.builder().id(CASE_ID).debtorId(null).debtIds(List.of()).build();
    when(caseServiceClient.getCase(CASE_ID)).thenReturn(caseDto);
    when(caseServiceClient.getParties(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getMeasures(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getObjections(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getJournalEntries(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getJournalNotes(CASE_ID)).thenReturn(List.of());

    String view = controller.caseDetail(CASE_ID, session, model);

    assertThat(view).isEqualTo("cases/detail");
    assertThat((List<?>) model.asMap().get("debts")).isEmpty();
  }

  @Test
  void caseDetail_withInvalidDebtorId_usesRawId() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    CaseDto caseDto = CaseDto.builder().id(CASE_ID).debtorId("not-a-uuid").debtIds(null).build();
    when(caseServiceClient.getCase(CASE_ID)).thenReturn(caseDto);
    when(caseServiceClient.getParties(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getMeasures(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getObjections(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getJournalEntries(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getJournalNotes(CASE_ID)).thenReturn(List.of());

    String view = controller.caseDetail(CASE_ID, session, model);

    assertThat(view).isEqualTo("cases/detail");
    assertThat(model.asMap().get("debtorDisplay")).isEqualTo("not-a-uuid");
  }

  @Test
  void caseDetail_whenCaseServiceFails_setsBackendError() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    when(caseServiceClient.getCase(CASE_ID)).thenThrow(new RuntimeException("case-service down"));
    when(messageSource.getMessage(anyString(), any(), anyString(), any()))
        .thenReturn("Case detail unavailable");

    String view = controller.caseDetail(CASE_ID, session, model);

    assertThat(view).isEqualTo("cases/detail");
    assertThat(model.asMap()).containsKey("backendError");
  }

  @Test
  void caseDetail_whenPartialSubresourcesFail_stillLoadsPage() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    CaseDto caseDto = CaseDto.builder().id(CASE_ID).debtorId(null).debtIds(List.of()).build();
    when(caseServiceClient.getCase(CASE_ID)).thenReturn(caseDto);
    when(caseServiceClient.getParties(CASE_ID)).thenThrow(new RuntimeException("parties fail"));
    when(caseServiceClient.getMeasures(CASE_ID)).thenThrow(new RuntimeException("measures fail"));
    when(caseServiceClient.getObjections(CASE_ID))
        .thenThrow(new RuntimeException("objections fail"));
    when(caseServiceClient.getJournalEntries(CASE_ID))
        .thenReturn(List.of(CaseJournalEntryDto.builder().build()));
    when(caseServiceClient.getJournalNotes(CASE_ID))
        .thenReturn(List.of(CaseJournalNoteDto.builder().build()));

    String view = controller.caseDetail(CASE_ID, session, model);

    assertThat(view).isEqualTo("cases/detail");
    assertThat((List<?>) model.asMap().get("parties")).isEmpty();
    assertThat((List<?>) model.asMap().get("measures")).isEmpty();
    assertThat((List<?>) model.asMap().get("objections")).isEmpty();
  }

  @Test
  void caseDetail_withAllMeasuresAndObjections_populatesAll() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    CaseDto caseDto = CaseDto.builder().id(CASE_ID).debtIds(List.of()).build();
    when(caseServiceClient.getCase(CASE_ID)).thenReturn(caseDto);
    when(caseServiceClient.getParties(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getMeasures(CASE_ID))
        .thenReturn(List.of(CollectionMeasureDto.builder().build()));
    when(caseServiceClient.getObjections(CASE_ID))
        .thenReturn(List.of(ObjectionDto.builder().build()));
    when(caseServiceClient.getJournalEntries(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getJournalNotes(CASE_ID)).thenReturn(List.of());

    String view = controller.caseDetail(CASE_ID, session, model);

    assertThat(view).isEqualTo("cases/detail");
    assertThat((List<?>) model.asMap().get("measures")).hasSize(1);
    assertThat((List<?>) model.asMap().get("objections")).hasSize(1);
  }

  @Test
  void caseDetail_whenPartyNameUnavailable_usesPartyUuidFallback() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    CaseDto caseDto = CaseDto.builder().id(CASE_ID).debtIds(List.of()).build();
    when(caseServiceClient.getCase(CASE_ID)).thenReturn(caseDto);
    when(caseServiceClient.getParties(CASE_ID))
        .thenReturn(List.of(CasePartyDto.builder().personId(PARTY_ID).build()));
    when(personRegistryClient.getDisplayName(PARTY_ID)).thenReturn("—");
    when(caseServiceClient.getMeasures(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getObjections(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getJournalEntries(CASE_ID)).thenReturn(List.of());
    when(caseServiceClient.getJournalNotes(CASE_ID)).thenReturn(List.of());

    String view = controller.caseDetail(CASE_ID, session, model);

    assertThat(view).isEqualTo("cases/detail");
    assertThat(partyDisplayNames()).containsEntry(PARTY_ID, PARTY_ID.toString());
  }

  private CaseworkerIdentity caseworker() {
    return CaseworkerIdentity.builder().id("u1").name("Test").role("CASEWORKER").build();
  }
}
