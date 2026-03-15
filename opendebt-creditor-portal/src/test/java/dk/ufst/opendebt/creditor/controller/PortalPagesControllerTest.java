package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.client.RestPage;
import dk.ufst.opendebt.creditor.dto.PortalDebtDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class PortalPagesControllerTest {

  @Mock private DebtServiceClient debtServiceClient;
  @Mock private PortalSessionService portalSessionService;

  @InjectMocks private PortalPagesController controller;

  @Test
  void fordringer_returnsFordringerViewName() {
    UUID creditorOrgId = UUID.randomUUID();
    when(portalSessionService.resolveActingCreditor(any(), any())).thenReturn(creditorOrgId);

    List<PortalDebtDto> debtList =
        List.of(
            PortalDebtDto.builder()
                .id(UUID.randomUUID())
                .debtTypeCode("SKAT")
                .principalAmount(new BigDecimal("45000.00"))
                .outstandingBalance(new BigDecimal("32750.00"))
                .dueDate(LocalDate.of(2025, 7, 1))
                .status("IN_COLLECTION")
                .build());
    RestPage<PortalDebtDto> page = new RestPage<>(debtList, 0, 20, 1, 1);
    when(debtServiceClient.listDebts(creditorOrgId)).thenReturn(page);

    Model model = new ConcurrentModel();
    MockHttpSession session = new MockHttpSession();
    String viewName = controller.fordringer(model, session);

    assertThat(viewName).isEqualTo("fordringer");
    @SuppressWarnings("unchecked")
    List<PortalDebtDto> debts = (List<PortalDebtDto>) model.getAttribute("debts");
    assertThat(debts).hasSize(1);
    assertThat(debts.get(0).getDebtTypeCode()).isEqualTo("SKAT");
  }

  @Test
  void fordringer_returnsEmptyList_whenServiceUnavailable() {
    when(portalSessionService.resolveActingCreditor(any(), any())).thenReturn(UUID.randomUUID());
    when(debtServiceClient.listDebts(any())).thenThrow(new RuntimeException("Connection refused"));

    Model model = new ConcurrentModel();
    MockHttpSession session = new MockHttpSession();
    String viewName = controller.fordringer(model, session);

    assertThat(viewName).isEqualTo("fordringer");
    @SuppressWarnings("unchecked")
    List<PortalDebtDto> debts = (List<PortalDebtDto>) model.getAttribute("debts");
    assertThat(debts).isEmpty();
  }

  @Test
  void sager_returnsSagerViewName() {
    assertThat(controller.sager()).isEqualTo("sager");
  }
}
