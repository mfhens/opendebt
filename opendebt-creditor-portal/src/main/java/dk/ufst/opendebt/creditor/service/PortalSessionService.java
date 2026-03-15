package dk.ufst.opendebt.creditor.service;

import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.dto.AccessResolutionRequest;
import dk.ufst.opendebt.creditor.dto.AccessResolutionResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves and caches the acting creditor for the current portal session. Supports
 * acting-on-behalf-of (W3-SEC-01) where a creditor organisation may represent another creditor
 * within a hierarchy.
 *
 * <p>For demo/development purposes the acting creditor can be set via:
 *
 * <ul>
 *   <li>Request parameter {@code ?actAs={creditorOrgId}}
 *   <li>A previously stored session attribute
 *   <li>Fallback to the demo creditor org ID
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalSessionService {

  static final String SESSION_ACTING_CREDITOR = "actingCreditorOrgId";
  static final String SESSION_REPRESENTED_CREDITOR = "representedCreditorOrgId";

  /**
   * Hardcoded demo creditor org ID. In production this would come from the OAuth2 security context.
   */
  static final UUID DEMO_CREDITOR_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final CreditorServiceClient creditorServiceClient;

  /**
   * Resolves the acting creditor org ID for the current session.
   *
   * <p>If an {@code actAsParam} is provided, validates the acting-on-behalf-of relationship via the
   * creditor service. If the session already contains an acting creditor, it is returned directly.
   * Otherwise, falls back to the demo creditor org ID.
   *
   * @param actAsParam the optional {@code ?actAs=} request parameter (may be {@code null})
   * @param session the current HTTP session
   * @return the resolved acting creditor org ID
   */
  public UUID resolveActingCreditor(String actAsParam, HttpSession session) {
    // If an explicit actAs parameter is provided, attempt to resolve the acting-on-behalf-of
    if (actAsParam != null && !actAsParam.isBlank()) {
      return resolveActingOnBehalfOf(actAsParam, session);
    }

    // Check if the session already has a resolved acting creditor
    UUID sessionCreditor = (UUID) session.getAttribute(SESSION_ACTING_CREDITOR);
    if (sessionCreditor != null) {
      return sessionCreditor;
    }

    // Fallback to demo creditor
    session.setAttribute(SESSION_ACTING_CREDITOR, DEMO_CREDITOR_ORG_ID);
    return DEMO_CREDITOR_ORG_ID;
  }

  /**
   * Returns the represented creditor org ID if the session has an acting-on-behalf-of relationship,
   * or {@code null} if the user acts as themselves.
   */
  public UUID getRepresentedCreditor(HttpSession session) {
    return (UUID) session.getAttribute(SESSION_REPRESENTED_CREDITOR);
  }

  /**
   * Resolves an acting-on-behalf-of relationship. Returns an {@link AccessResolutionResponse} that
   * the caller can inspect for denial reasons. Returns {@code null} on backend failure (graceful
   * fallback).
   */
  public AccessResolutionResponse tryResolveAccess(UUID actingOrgId, UUID representedOrgId) {
    try {
      AccessResolutionRequest request =
          AccessResolutionRequest.builder()
              .channelType("PORTAL")
              .presentedIdentity(actingOrgId.toString())
              .representedCreditorOrgId(representedOrgId)
              .build();
      return creditorServiceClient.resolveAccess(request);
    } catch (Exception ex) {
      log.warn(
          "Creditor service unavailable for access resolution, falling back: {}", ex.getMessage());
      return null;
    }
  }

  /**
   * Clears the acting-on-behalf-of state from the session, reverting to the default creditor.
   *
   * @param session the HTTP session
   */
  public void clearActingOnBehalfOf(HttpSession session) {
    session.removeAttribute(SESSION_ACTING_CREDITOR);
    session.removeAttribute(SESSION_REPRESENTED_CREDITOR);
  }

  private UUID resolveActingOnBehalfOf(String actAsParam, HttpSession session) {
    UUID representedOrgId;
    try {
      representedOrgId = UUID.fromString(actAsParam);
    } catch (IllegalArgumentException ex) {
      log.warn("Invalid actAs parameter: {}", actAsParam);
      session.setAttribute(SESSION_ACTING_CREDITOR, DEMO_CREDITOR_ORG_ID);
      return DEMO_CREDITOR_ORG_ID;
    }

    UUID actingOrgId = DEMO_CREDITOR_ORG_ID;

    AccessResolutionResponse response = tryResolveAccess(actingOrgId, representedOrgId);
    if (response != null && response.isAllowed()) {
      UUID resolvedActing =
          response.getActingCreditorOrgId() != null
              ? response.getActingCreditorOrgId()
              : actingOrgId;
      session.setAttribute(SESSION_ACTING_CREDITOR, resolvedActing);
      session.setAttribute(SESSION_REPRESENTED_CREDITOR, response.getRepresentedCreditorOrgId());
      log.info(
          "Acting-on-behalf-of resolved: acting={}, represented={}",
          resolvedActing,
          response.getRepresentedCreditorOrgId());
      return resolvedActing;
    } else if (response != null && !response.isAllowed()) {
      log.warn(
          "Access denied for acting-on-behalf-of: reason={}, message={}",
          response.getReasonCode(),
          response.getMessage());
      // Fall back to own identity – the controller can check the denial
      session.setAttribute(SESSION_ACTING_CREDITOR, actingOrgId);
      session.removeAttribute(SESSION_REPRESENTED_CREDITOR);
      return actingOrgId;
    }

    // Backend unavailable – fall back to demo creditor
    session.setAttribute(SESSION_ACTING_CREDITOR, actingOrgId);
    return actingOrgId;
  }
}
