package dk.ufst.opendebt.debtservice.service;

import java.util.List;
import java.util.UUID;

import dk.ufst.opendebt.debtservice.dto.ActiveFordringResponseDto;

/**
 * Internal service for resolving the active fordringer list for a debtor.
 *
 * <p>Consumed by {@code GET /internal/debtors/{debtorId}/fordringer/active} (TB-040). Payment-
 * service calls this endpoint during dækningsrækkefølge calculation (P057) instead of relying on
 * the interim {@code DaekningFordringRepository} cache in payment-service.
 */
public interface ActiveFordringService {

  /**
   * Returns all active fordringer for the given debtor, ordered by sekvensNummer ASC then by
   * applicationTimestamp ASC (FIFO tie-breaker), consistent with P057 § dækningsrækkefølge.
   *
   * <p>"Active" means: {@code outstandingBalance > 0} AND status not in {PAID, WRITTEN_OFF,
   * CANCELLED}.
   *
   * @param debtorPersonId person-registry UUID — never CPR
   * @return ordered list of active fordring projections; empty list when none found
   */
  List<ActiveFordringResponseDto> getActiveFordringer(UUID debtorPersonId);
}
