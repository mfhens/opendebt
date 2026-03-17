package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.dto.PortalCreditorDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class ClaimantSelectionControllerTest {

  private static final UUID TEST_CREDITOR_ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private CreditorServiceClient creditorServiceClient;
  @Mock private PortalSessionService portalSessionService;

  @InjectMocks private ClaimantSelectionController controller;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  @Test
  void showSelection_returnsSelectionView_withCreditors() {
    List<PortalCreditorDto> creditors =
        List.of(
            PortalCreditorDto.builder()
                .id(UUID.randomUUID())
                .creditorOrgId(TEST_CREDITOR_ORG_ID)
                .externalCreditorId("SKAT-001")
                .activityStatus("ACTIVE")
                .build());
    when(creditorServiceClient.listAllActive()).thenReturn(creditors);

    Model model = new ConcurrentModel();
    String viewName = controller.showSelection(model, session);

    assertThat(viewName).isEqualTo("vaelg-fordringshaver");
    assertThat(model.getAttribute("creditors")).isEqualTo(creditors);
    assertThat(model.getAttribute("currentPage")).isEqualTo("claimant-selection");
  }

  @Test
  @SuppressWarnings("unchecked")
  void showSelection_returnsEmptyList_whenNoCreditorsFound() {
    when(creditorServiceClient.listAllActive()).thenReturn(List.of());

    Model model = new ConcurrentModel();
    String viewName = controller.showSelection(model, session);

    assertThat(viewName).isEqualTo("vaelg-fordringshaver");
    assertThat((List<PortalCreditorDto>) model.getAttribute("creditors")).isEmpty();
  }

  @Test
  void selectCreditor_setsSessionAndRedirectsToHome() {
    String viewName = controller.selectCreditor(TEST_CREDITOR_ORG_ID, session);

    assertThat(viewName).isEqualTo("redirect:/");
    verify(portalSessionService).setActingCreditor(TEST_CREDITOR_ORG_ID, session);
  }
}
