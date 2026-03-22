package dk.ufst.opendebt.debtservice.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Component;

import dk.ufst.opendebt.common.security.AuthContext;
import dk.ufst.opendebt.common.security.DebtorAccessChecker;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements debtor access authorization for debt queries.
 *
 * <p>Authorization Rules (from petition048 solution architecture):
 *
 * <ul>
 *   <li><b>Rule 4.1:</b> Citizens can view/query debts where debtor_person_id matches their
 *       person_id JWT claim.
 *   <li><b>Rule 6.1:</b> Admins have unrestricted access to all debts.
 * </ul>
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
public class DebtorAccessCheckerImpl implements DebtorAccessChecker {

  private final DebtRepository debtRepository;

  /**
   * Checks if the authenticated user (citizen) can access a specific debt.
   *
   * @param debtId The debt ID to check access for
   * @param authContext The extracted JWT authentication context
   * @return true if access is allowed, false otherwise
   */
  @Override
  public boolean canAccessDebt(UUID debtId, AuthContext authContext) {
    // Rule 6.1: Admins bypass all checks
    if (authContext.isAdmin()) {
      log.trace("Admin access granted to debt: {}", debtId);
      return true;
    }

    // Supervisors and caseworkers access debts through case-service, not directly
    if (authContext.hasRole("SUPERVISOR") || authContext.hasRole("CASEWORKER")) {
      log.trace("Supervisor/Caseworker access granted to debt: {}", debtId);
      return true;
    }

    // Rule 4.1: Citizens can only access their own debts
    if (authContext.hasRole("CITIZEN")) {
      UUID citizenPersonId = authContext.getPersonId();
      if (citizenPersonId == null) {
        log.warn("CITIZEN role without person_id claim in JWT for debt: {}", debtId);
        return false;
      }

      DebtEntity debt = debtRepository.findById(debtId).orElse(null);
      if (debt == null) {
        log.debug("Debt not found for access check: {}", debtId);
        return false; // Debt doesn't exist - deny access
      }

      boolean hasAccess = citizenPersonId.equals(debt.getDebtorPersonId());
      if (!hasAccess) {
        log.warn(
            "Citizen {} attempted to access debt {} belonging to {}",
            citizenPersonId,
            debtId,
            debt.getDebtorPersonId());
      }
      return hasAccess;
    }

    // Creditors should not directly access debts (they access claims via creditor-service)
    if (authContext.hasRole("CREDITOR")) {
      log.warn("Creditor attempted direct debt access: {}", debtId);
      return false;
    }

    // Unknown role - deny access
    log.warn("Unknown role attempted debt access: {} for debt: {}", authContext.getRoles(), debtId);
    return false;
  }
}
