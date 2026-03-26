package dk.ufst.opendebt.caseworker.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;

class CaseworkerSessionServiceTest {

  private CaseworkerSessionService service;
  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    service = new CaseworkerSessionService();
    session = new MockHttpSession();
  }

  @Test
  void getCurrentCaseworker_returnsNullWhenNotSet() {
    assertThat(service.getCurrentCaseworker(session)).isNull();
  }

  @Test
  void setCurrentCaseworker_storesIdentityInSession() {
    CaseworkerIdentity identity =
        CaseworkerIdentity.builder().id("anna").name("Anna").role("CASEWORKER").build();

    service.setCurrentCaseworker(identity, session);

    assertThat(service.getCurrentCaseworker(session)).isEqualTo(identity);
  }

  @Test
  void clearSession_removesIdentity() {
    CaseworkerIdentity identity =
        CaseworkerIdentity.builder().id("anna").name("Anna").role("CASEWORKER").build();
    service.setCurrentCaseworker(identity, session);

    service.clearSession(session);

    assertThat(service.getCurrentCaseworker(session)).isNull();
  }

  @Test
  void clearSession_onEmptySession_doesNotThrow() {
    service.clearSession(session);
    assertThat(service.getCurrentCaseworker(session)).isNull();
  }
}
