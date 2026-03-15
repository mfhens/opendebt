package dk.ufst.opendebt.creditor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.dto.AccessResolutionResponse;

@ExtendWith(MockitoExtension.class)
class PortalSessionServiceTest {

  @Mock private CreditorServiceClient creditorServiceClient;

  @InjectMocks private PortalSessionService service;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  @Test
  void resolveActingCreditor_returnsDemoCreditor_whenNoParamsAndNoSession() {
    UUID result = service.resolveActingCreditor(null, session);

    assertThat(result).isEqualTo(PortalSessionService.DEMO_CREDITOR_ORG_ID);
    assertThat(session.getAttribute(PortalSessionService.SESSION_ACTING_CREDITOR))
        .isEqualTo(PortalSessionService.DEMO_CREDITOR_ORG_ID);
  }

  @Test
  void resolveActingCreditor_returnsSessionCreditor_whenAlreadySet() {
    UUID existingCreditor = UUID.randomUUID();
    session.setAttribute(PortalSessionService.SESSION_ACTING_CREDITOR, existingCreditor);

    UUID result = service.resolveActingCreditor(null, session);

    assertThat(result).isEqualTo(existingCreditor);
  }

  @Test
  void resolveActingCreditor_resolvesActAsParam_whenAccessAllowed() {
    UUID representedOrgId = UUID.randomUUID();
    UUID actingOrgId = UUID.randomUUID();

    AccessResolutionResponse response =
        AccessResolutionResponse.builder()
            .allowed(true)
            .actingCreditorOrgId(actingOrgId)
            .representedCreditorOrgId(representedOrgId)
            .build();
    when(creditorServiceClient.resolveAccess(any())).thenReturn(response);

    UUID result = service.resolveActingCreditor(representedOrgId.toString(), session);

    assertThat(result).isEqualTo(actingOrgId);
    assertThat(session.getAttribute(PortalSessionService.SESSION_REPRESENTED_CREDITOR))
        .isEqualTo(representedOrgId);
  }

  @Test
  void resolveActingCreditor_fallsToDemoCreditor_whenAccessDenied() {
    UUID representedOrgId = UUID.randomUUID();

    AccessResolutionResponse response =
        AccessResolutionResponse.builder()
            .allowed(false)
            .reasonCode("UNAUTHORIZED")
            .message("Ikke tilladt")
            .build();
    when(creditorServiceClient.resolveAccess(any())).thenReturn(response);

    UUID result = service.resolveActingCreditor(representedOrgId.toString(), session);

    assertThat(result).isEqualTo(PortalSessionService.DEMO_CREDITOR_ORG_ID);
    assertThat(session.getAttribute(PortalSessionService.SESSION_REPRESENTED_CREDITOR)).isNull();
  }

  @Test
  void resolveActingCreditor_fallsToDemoCreditor_whenBackendUnavailable() {
    UUID representedOrgId = UUID.randomUUID();
    when(creditorServiceClient.resolveAccess(any()))
        .thenThrow(new RuntimeException("Connection refused"));

    UUID result = service.resolveActingCreditor(representedOrgId.toString(), session);

    assertThat(result).isEqualTo(PortalSessionService.DEMO_CREDITOR_ORG_ID);
  }

  @Test
  void resolveActingCreditor_fallsToDemoCreditor_whenActAsParamInvalid() {
    UUID result = service.resolveActingCreditor("not-a-uuid", session);

    assertThat(result).isEqualTo(PortalSessionService.DEMO_CREDITOR_ORG_ID);
  }

  @Test
  void getRepresentedCreditor_returnsNull_whenNotSet() {
    assertThat(service.getRepresentedCreditor(session)).isNull();
  }

  @Test
  void getRepresentedCreditor_returnsStoredValue() {
    UUID represented = UUID.randomUUID();
    session.setAttribute(PortalSessionService.SESSION_REPRESENTED_CREDITOR, represented);

    assertThat(service.getRepresentedCreditor(session)).isEqualTo(represented);
  }

  @Test
  void clearActingOnBehalfOf_removesSessionAttributes() {
    session.setAttribute(PortalSessionService.SESSION_ACTING_CREDITOR, UUID.randomUUID());
    session.setAttribute(PortalSessionService.SESSION_REPRESENTED_CREDITOR, UUID.randomUUID());

    service.clearActingOnBehalfOf(session);

    assertThat(session.getAttribute(PortalSessionService.SESSION_ACTING_CREDITOR)).isNull();
    assertThat(session.getAttribute(PortalSessionService.SESSION_REPRESENTED_CREDITOR)).isNull();
  }

  @Test
  void tryResolveAccess_returnsNull_whenBackendFails() {
    when(creditorServiceClient.resolveAccess(any()))
        .thenThrow(new RuntimeException("Connection refused"));

    AccessResolutionResponse result =
        service.tryResolveAccess(UUID.randomUUID(), UUID.randomUUID());

    assertThat(result).isNull();
  }

  @Test
  void tryResolveAccess_returnsResponse_whenBackendAvailable() {
    AccessResolutionResponse expected = AccessResolutionResponse.builder().allowed(true).build();
    when(creditorServiceClient.resolveAccess(any())).thenReturn(expected);

    AccessResolutionResponse result =
        service.tryResolveAccess(UUID.randomUUID(), UUID.randomUUID());

    assertThat(result).isEqualTo(expected);
  }
}
