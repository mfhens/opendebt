package dk.ufst.opendebt.caseservice.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Component;

import dk.ufst.opendebt.caseservice.entity.CaseEntity;
import dk.ufst.opendebt.caseservice.entity.CaseSensitivity;
import dk.ufst.opendebt.caseservice.repository.CaseRepository;
import dk.ufst.opendebt.common.security.AuthContext;
import dk.ufst.opendebt.common.security.CaseAccessChecker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of case access authorization checking. Enforces Rules 1.1, 1.2, 2.1, 2.2, 5.3, 5.4
 * from petition048.
 *
 * <p>Per ADR-0007, this service validates authorization at the case-service boundary.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaseAccessCheckerImpl implements CaseAccessChecker {

  private final CaseRepository caseRepository;

  @Override
  public boolean canAccessCase(UUID caseId, AuthContext authContext) {
    // Admin bypass (Rule 6.1)
    if (authContext.isAdmin()) {
      log.trace("Admin access granted for case {}", caseId);
      return true;
    }

    // Supervisor bypass (Rule 2.1, 2.2)
    if (authContext.hasRole("SUPERVISOR")) {
      log.trace("Supervisor access granted for case {}", caseId);
      return true;
    }

    // Caseworker filtering (Rule 1.1, 1.2, 5.2, 5.3)
    if (authContext.hasRole("CASEWORKER")) {
      CaseEntity caseEntity = caseRepository.findById(caseId).orElse(null);
      if (caseEntity == null) {
        log.warn("Case {} not found", caseId);
        return false;
      }

      // Check assignment
      boolean isAssigned = authContext.getUserId().equals(caseEntity.getPrimaryCaseworkerId());

      if (!isAssigned) {
        log.debug("Caseworker {} not assigned to case {}", authContext.getUserId(), caseId);
        return false;
      }

      // Sensitivity filtering (W9-RBAC-02)
      CaseSensitivity sensitivity =
          caseEntity.getSensitivity() != null
              ? caseEntity.getSensitivity()
              : CaseSensitivity.NORMAL;

      // Rule 5.3: CONFIDENTIAL cases are restricted to supervisors and admins only
      if (sensitivity == CaseSensitivity.CONFIDENTIAL) {
        log.warn(
            "Caseworker {} denied access to CONFIDENTIAL case {}", authContext.getUserId(), caseId);
        return false;
      }

      // Rule 5.2: VIP cases require HANDLE_VIP_CASES capability
      if (sensitivity == CaseSensitivity.VIP && !authContext.hasCapability("HANDLE_VIP_CASES")) {
        log.warn(
            "Caseworker {} lacks HANDLE_VIP_CASES capability for VIP case {}",
            authContext.getUserId(),
            caseId);
        return false;
      }

      // Rule 5.2: PEP cases require HANDLE_PEP_CASES capability
      if (sensitivity == CaseSensitivity.PEP && !authContext.hasCapability("HANDLE_PEP_CASES")) {
        log.warn(
            "Caseworker {} lacks HANDLE_PEP_CASES capability for PEP case {}",
            authContext.getUserId(),
            caseId);
        return false;
      }

      log.trace("Caseworker access granted for case {}", caseId);
      return true;
    }

    // Citizens and creditors cannot directly access cases (Rule 3.3, 4.3)
    log.debug(
        "Access denied for user {} with roles {} to case {}",
        authContext.getUserId(),
        authContext.getRoles(),
        caseId);
    return false;
  }
}
