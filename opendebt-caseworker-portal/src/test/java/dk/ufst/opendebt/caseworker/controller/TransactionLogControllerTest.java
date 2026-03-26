package dk.ufst.opendebt.caseworker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import dk.ufst.opendebt.caseworker.client.PaymentServiceClient;
import dk.ufst.opendebt.caseworker.client.RestPage;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.dto.PortalLedgerEntryDto;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;

@ExtendWith(MockitoExtension.class)
class TransactionLogControllerTest {

  @Mock private PaymentServiceClient paymentServiceClient;
  @Mock private CaseworkerSessionService sessionService;
  @InjectMocks private TransactionLogController controller;

  private final MockHttpSession session = new MockHttpSession();
  private final Model model = new ExtendedModelMap();
  private static final UUID CASE_ID = UUID.randomUUID();
  private static final UUID DEBT_ID = UUID.randomUUID();

  // --- casePosteringslog ---

  @Test
  void casePosteringslog_whenNoSession_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view = controller.casePosteringslog(CASE_ID, null, null, null, 0, 50, session, model);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void casePosteringslog_success_populatesModel() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    PortalLedgerEntryDto entry = PortalLedgerEntryDto.builder().debtId(DEBT_ID).build();
    RestPage<PortalLedgerEntryDto> page = new RestPage<>(List.of(entry), 0, 50, 1, 1);
    when(paymentServiceClient.getLedgerByCase(
            eq(CASE_ID), isNull(), isNull(), isNull(), anyInt(), anyInt()))
        .thenReturn(page);

    String view = controller.casePosteringslog(CASE_ID, null, null, null, 0, 50, session, model);

    assertThat(view).isEqualTo("fragments/posteringslog :: posteringslog");
    assertThat((List<?>) model.asMap().get("ledgerEntries")).hasSize(1);
    assertThat(model.asMap()).containsKey("ledgerPage");
  }

  @Test
  void casePosteringslog_withFilters_passesFiltersToModel() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    RestPage<PortalLedgerEntryDto> page = new RestPage<>(List.of(), 0, 50, 0, 0);
    when(paymentServiceClient.getLedgerByCase(
            any(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
        .thenReturn(page);

    String view =
        controller.casePosteringslog(
            CASE_ID, "INTEREST", "2025-01-01", "2025-12-31", 0, 50, session, model);

    assertThat(view).isEqualTo("fragments/posteringslog :: posteringslog");
    assertThat(model.asMap()).containsEntry("category", "INTEREST");
    assertThat(model.asMap()).containsEntry("fromDate", "2025-01-01");
  }

  @Test
  void casePosteringslog_whenBackendFails_setsEmptyList() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    when(paymentServiceClient.getLedgerByCase(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenThrow(new RuntimeException("timeout"));

    String view = controller.casePosteringslog(CASE_ID, null, null, null, 0, 50, session, model);

    assertThat(view).isEqualTo("fragments/posteringslog :: posteringslog");
    assertThat((List<?>) model.asMap().get("ledgerEntries")).isEmpty();
  }

  // --- debtPosteringslog ---

  @Test
  void debtPosteringslog_whenNoSession_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view =
        controller.debtPosteringslog(CASE_ID, DEBT_ID, null, null, null, 0, 50, session, model);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void debtPosteringslog_success_populatesModel() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    RestPage<PortalLedgerEntryDto> page = new RestPage<>(List.of(), 0, 50, 0, 0);
    when(paymentServiceClient.getLedgerByDebt(
            eq(DEBT_ID), isNull(), isNull(), isNull(), anyInt(), anyInt()))
        .thenReturn(page);

    String view =
        controller.debtPosteringslog(CASE_ID, DEBT_ID, null, null, null, 0, 50, session, model);

    assertThat(view).isEqualTo("fragments/posteringslog :: posteringslog");
    assertThat(model.asMap()).containsEntry("debtId", DEBT_ID);
  }

  @Test
  void debtPosteringslog_whenBackendFails_setsEmptyList() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    when(paymentServiceClient.getLedgerByDebt(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenThrow(new RuntimeException("timeout"));

    String view =
        controller.debtPosteringslog(CASE_ID, DEBT_ID, null, null, null, 0, 50, session, model);

    assertThat(view).isEqualTo("fragments/posteringslog :: posteringslog");
    assertThat((List<?>) model.asMap().get("ledgerEntries")).isEmpty();
  }

  private CaseworkerIdentity caseworker() {
    return CaseworkerIdentity.builder().id("u1").name("Test").role("CASEWORKER").build();
  }
}
