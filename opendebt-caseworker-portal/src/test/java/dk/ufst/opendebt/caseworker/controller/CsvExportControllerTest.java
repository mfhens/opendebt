package dk.ufst.opendebt.caseworker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import dk.ufst.opendebt.caseworker.client.PaymentServiceClient;
import dk.ufst.opendebt.caseworker.client.RestPage;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.dto.PortalLedgerEntryDto;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;

@ExtendWith(MockitoExtension.class)
class CsvExportControllerTest {

  @Mock private PaymentServiceClient paymentServiceClient;
  @Mock private CaseworkerSessionService sessionService;
  @InjectMocks private CsvExportController controller;

  private final MockHttpSession session = new MockHttpSession();
  private static final UUID CASE_ID = UUID.randomUUID();
  private static final UUID DEBT_ID = UUID.randomUUID();

  @Test
  void exportCsv_whenNoSession_redirects() throws Exception {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);
    MockHttpServletResponse response = new MockHttpServletResponse();

    controller.exportCsv(CASE_ID, DEBT_ID, null, null, null, session, response);

    // sendRedirect sets a redirect, no exception
    assertThat(response.getRedirectedUrl()).isNotNull();
  }

  @Test
  void exportCsv_success_writesCsvWithHeaderAndRows() throws Exception {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    PortalLedgerEntryDto entry =
        PortalLedgerEntryDto.builder()
            .debtId(DEBT_ID)
            .accountCode("1000")
            .accountName("Kapital")
            .entryType("DEBIT")
            .amount(new BigDecimal("1234.56"))
            .entryCategory("PRINCIPAL")
            .reference("REF-001")
            .description("Betaling")
            .build();
    RestPage<PortalLedgerEntryDto> page = new RestPage<>(List.of(entry), 0, 10000, 1, 1);
    when(paymentServiceClient.getLedgerByDebt(
            any(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
        .thenReturn(page);

    MockHttpServletResponse response = new MockHttpServletResponse();
    controller.exportCsv(CASE_ID, DEBT_ID, null, null, null, session, response);

    String csvContent = response.getContentAsString();
    assertThat(response.getContentType()).contains("text/csv");
    assertThat(response.getHeader("Content-Disposition")).contains("posteringslog-" + DEBT_ID);
    assertThat(csvContent).contains("Vaerdidag");
    assertThat(csvContent).contains("1234.56");
    assertThat(csvContent).contains("Kapital");
  }

  @Test
  void exportCsv_withFieldsContainingSemicolon_escapesCsvProperly() throws Exception {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    PortalLedgerEntryDto entry =
        PortalLedgerEntryDto.builder()
            .accountName("Name;With;Semicolons")
            .description("Desc \"quoted\"")
            .reference(null)
            .amount(BigDecimal.ZERO)
            .debtId(DEBT_ID)
            .build();
    RestPage<PortalLedgerEntryDto> page = new RestPage<>(List.of(entry), 0, 10000, 1, 1);
    when(paymentServiceClient.getLedgerByDebt(
            any(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
        .thenReturn(page);

    MockHttpServletResponse response = new MockHttpServletResponse();
    controller.exportCsv(CASE_ID, DEBT_ID, null, null, null, session, response);

    String csvContent = response.getContentAsString();
    // Fields with semicolons are quoted
    assertThat(csvContent).contains("\"Name;With;Semicolons\"");
    // Fields with double-quotes are escaped
    assertThat(csvContent).contains("\"\"quoted\"\"");
  }

  @Test
  void exportCsv_withAllNullFields_producesEmptyFieldsGracefully() throws Exception {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    PortalLedgerEntryDto entry = PortalLedgerEntryDto.builder().build();
    RestPage<PortalLedgerEntryDto> page = new RestPage<>(List.of(entry), 0, 10000, 1, 1);
    when(paymentServiceClient.getLedgerByDebt(
            any(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
        .thenReturn(page);

    MockHttpServletResponse response = new MockHttpServletResponse();
    controller.exportCsv(CASE_ID, DEBT_ID, null, null, null, session, response);

    String csvContent = response.getContentAsString();
    assertThat(csvContent).contains("Vaerdidag"); // header is present
  }

  private CaseworkerIdentity caseworker() {
    return CaseworkerIdentity.builder().id("u1").name("Test").role("CASEWORKER").build();
  }
}
