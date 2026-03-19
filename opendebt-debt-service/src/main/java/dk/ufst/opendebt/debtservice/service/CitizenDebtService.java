package dk.ufst.opendebt.debtservice.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import dk.ufst.opendebt.debtservice.dto.CitizenDebtSummaryResponse;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;

public interface CitizenDebtService {

  /**
   * Get debt summary for a citizen by person_id. Returns only public-facing information with NO
   * PII, NO creditor internals, NO readinessStatus.
   *
   * @param personId the technical person UUID from person-registry
   * @param status optional status filter
   * @param pageable pagination parameters
   * @return citizen debt summary with debts list and totals
   */
  CitizenDebtSummaryResponse getDebtSummary(
      UUID personId, DebtEntity.DebtStatus status, Pageable pageable);
}
