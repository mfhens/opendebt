package dk.ufst.opendebt.caseworker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import dk.ufst.opendebt.caseworker.client.CaseServiceClient;
import dk.ufst.opendebt.caseworker.client.DebtServiceClient;
import dk.ufst.opendebt.caseworker.client.PaymentServiceClient;
import dk.ufst.opendebt.caseworker.client.RestPage;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.dto.PortalLedgerEntryDto;
import dk.ufst.opendebt.caseworker.dto.PortalLedgerSummaryDto;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;
import dk.ufst.opendebt.common.dto.CaseDto;
import dk.ufst.opendebt.common.dto.DebtDto;

@ExtendWith(MockitoExtension.class)
class DebtDetailControllerTest {

  @Mock private CaseServiceClient caseServiceClient;
  @Mock private DebtServiceClient debtServiceClient;
  @Mock private PaymentServiceClient paymentServiceClient;
  @Mock private CaseworkerSessionService sessionService;
  @Mock private MessageSource messageSource;
  @InjectMocks private DebtDetailController controller;

  private final MockHttpSession session = new MockHttpSession();
  private final Model model = new ExtendedModelMap();
  private static final UUID CASE_ID = UUID.randomUUID();
  private static final UUID DEBT_ID = UUID.randomUUID();

  @Test
  void debtDetail_whenNoSession_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view = controller.debtDetail(CASE_ID, DEBT_ID, session, model);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void debtDetail_success_populatesAllModelAttributes() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    when(caseServiceClient.getCase(CASE_ID)).thenReturn(CaseDto.builder().id(CASE_ID).build());
    when(debtServiceClient.getDebt(DEBT_ID)).thenReturn(DebtDto.builder().build());
    RestPage<PortalLedgerEntryDto> ledger =
        new RestPage<>(List.of(PortalLedgerEntryDto.builder().build()), 0, 50, 1, 1);
    when(paymentServiceClient.getLedgerByDebt(
            any(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
        .thenReturn(ledger);
    PortalLedgerSummaryDto summary =
        PortalLedgerSummaryDto.builder()
            .totalBalance(BigDecimal.TEN)
            .principalBalance(BigDecimal.TEN)
            .build();
    when(paymentServiceClient.getLedgerSummary(DEBT_ID)).thenReturn(summary);

    String view = controller.debtDetail(CASE_ID, DEBT_ID, session, model);

    assertThat(view).isEqualTo("cases/debt-detail");
    assertThat(model.asMap()).containsKey("debt");
    assertThat(model.asMap()).containsKey("ledgerEntries");
    assertThat(model.asMap()).containsKey("summary");
    assertThat(model.asMap()).containsKey("caseDto");
  }

  @Test
  void debtDetail_whenBackendFails_setsBackendError() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    when(caseServiceClient.getCase(CASE_ID)).thenThrow(new RuntimeException("timeout"));
    when(messageSource.getMessage(anyString(), any(), anyString(), any()))
        .thenReturn("Debt detail unavailable");

    String view = controller.debtDetail(CASE_ID, DEBT_ID, session, model);

    assertThat(view).isEqualTo("cases/debt-detail");
    assertThat(model.asMap()).containsKey("backendError");
  }

  private CaseworkerIdentity caseworker() {
    return CaseworkerIdentity.builder().id("u1").name("Test").role("CASEWORKER").build();
  }
}
