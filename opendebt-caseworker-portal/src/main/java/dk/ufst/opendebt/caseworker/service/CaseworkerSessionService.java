package dk.ufst.opendebt.caseworker.service;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;

import lombok.extern.slf4j.Slf4j;

/**
 * Manages the current caseworker identity in the HTTP session. Used by the demo login flow to
 * simulate caseworker authentication before Keycloak is fully wired.
 */
@Slf4j
@Service
public class CaseworkerSessionService {

  static final String SESSION_CASEWORKER = "currentCaseworker";

  /** Stores the selected caseworker identity in the session. */
  public void setCurrentCaseworker(CaseworkerIdentity identity, HttpSession session) {
    session.setAttribute(SESSION_CASEWORKER, identity);
    log.info("Demo login: caseworker={} ({})", identity.getName(), identity.getRole());
  }

  /** Returns the current caseworker identity from the session, or {@code null} if none selected. */
  public CaseworkerIdentity getCurrentCaseworker(HttpSession session) {
    return (CaseworkerIdentity) session.getAttribute(SESSION_CASEWORKER);
  }

  /** Clears the caseworker identity from the session. */
  public void clearSession(HttpSession session) {
    session.removeAttribute(SESSION_CASEWORKER);
    log.info("Demo logout: session cleared");
  }
}
