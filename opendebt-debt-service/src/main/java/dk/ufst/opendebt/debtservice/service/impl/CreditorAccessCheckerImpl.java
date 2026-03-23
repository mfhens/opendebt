package dk.ufst.opendebt.debtservice.service.impl;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;
import dk.ufst.opendebt.common.security.AuthContext;
import dk.ufst.opendebt.common.security.CreditorAccessChecker;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements creditor organization-scoped access authorization for claim queries.
 *
 * <p>Authorization Rules (from petition048 solution architecture):
 *
 * <ul>
 *   <li><b>Rule 3.1:</b> Creditors can only view/query claims where creditor_org_id matches their
 *       organization_id JWT claim.
 *   <li><b>Rule 3.2:</b> Creditors can submit, modify (before acceptance), or withdraw their own
 *       claims.
 *   <li><b>Rule 6.1:</b> Admins have unrestricted access to all claims.
 * </ul>
 *
 * <p>Note: In the OpenDebt data model, a "claim" (fordring) submitted by a creditor becomes a
 * DebtEntity after validation. This checker validates access to DebtEntity records based on the
 * creditor_org_id field.
 *
 * <p>Security Pattern:
 *
 * <ul>
 *   <li>Service-layer authorization check (before business logic)
 *   <li>Query-level filtering via JPA Specifications (prevents in-memory filtering)
 *   <li>Audit trail via CLS integration (future: W9-RBAC-03)
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditorAccessCheckerImpl implements CreditorAccessChecker {

  private final DebtRepository debtRepository;
  private final ClsAuditClient clsAuditClient;

  /**
   * Checks if the authenticated creditor organization can access a specific claim (debt).
   *
   * @param claimId The claim/debt ID to check access for
   * @param authContext The extracted JWT authentication context
   * @return true if access is allowed, false otherwise
   */
  @Override
  public boolean canAccessClaim(UUID claimId, AuthContext authContext) {
    // Rule 6.1: Admins bypass all checks
    if (authContext.isAdmin()) {
      log.trace("Admin access granted to claim: {}", claimId);
      shipAuthorizationAuditEvent(claimId, authContext, true, "ADMIN_OVERRIDE");
      return true;
    }

    // Supervisors and caseworkers access claims through case-service, not directly
    if (authContext.hasRole("SUPERVISOR") || authContext.hasRole("CASEWORKER")) {
      log.trace("Supervisor/Caseworker access granted to claim: {}", claimId);
      shipAuthorizationAuditEvent(claimId, authContext, true, "CASE_CONTEXT_ACCESS");
      return true;
    }

    // Rule 3.1, 3.2: Creditors can only access claims belonging to their organization
    if (authContext.hasRole("CREDITOR")) {
      UUID creditorOrgId = authContext.getOrganizationId();
      if (creditorOrgId == null) {
        log.warn("CREDITOR role without organization_id claim in JWT for claim: {}", claimId);
        shipAuthorizationAuditEvent(claimId, authContext, false, "MISSING_ORGANIZATION_ID");
        return false;
      }

      DebtEntity debt = debtRepository.findById(claimId).orElse(null);
      if (debt == null) {
        log.debug("Claim not found for access check: {}", claimId);
        shipAuthorizationAuditEvent(claimId, authContext, false, "CLAIM_NOT_FOUND");
        return false; // Claim doesn't exist - deny access
      }

      boolean hasAccess = creditorOrgId.equals(debt.getCreditorOrgId());
      if (!hasAccess) {
        log.warn(
            "Creditor organization {} attempted to access claim {} belonging to {}",
            creditorOrgId,
            claimId,
            debt.getCreditorOrgId());
        shipAuthorizationAuditEvent(claimId, authContext, false, "CREDITOR_ORG_MISMATCH");
      } else {
        shipAuthorizationAuditEvent(claimId, authContext, true, "CREDITOR_ORG_MATCH");
      }
      return hasAccess;
    }

    // Citizens should not directly access claims (they access debts as debtors)
    if (authContext.hasRole("CITIZEN")) {
      log.warn("Citizen attempted direct claim access: {}", claimId);
      shipAuthorizationAuditEvent(claimId, authContext, false, "CITIZEN_DIRECT_CLAIM_ACCESS");
      return false;
    }

    // Unknown role - deny access
    log.warn(
        "Unknown role attempted claim access: {} for claim: {}", authContext.getRoles(), claimId);
    shipAuthorizationAuditEvent(claimId, authContext, false, "UNKNOWN_ROLE");
    return false;
  }

  private void shipAuthorizationAuditEvent(
      UUID claimId, AuthContext authContext, boolean granted, String reason) {
    ClsAuditEvent event =
        ClsAuditEvent.builder()
            .eventId(UUID.randomUUID())
            .timestamp(Instant.now())
            .serviceName("debt-service")
            .operation(granted ? "AUTHORIZE" : "DENY")
            .resourceType("claim")
            .resourceId(claimId)
            .userId(authContext.getUserId())
            .newValues(Map.of("granted", granted, "reason", reason))
            .build();
    clsAuditClient.shipEvent(event);
  }
}
