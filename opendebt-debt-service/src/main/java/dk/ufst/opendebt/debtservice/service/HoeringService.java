package dk.ufst.opendebt.debtservice.service;

import java.util.UUID;

import dk.ufst.opendebt.debtservice.entity.HoeringEntity;

/** Service for managing hearing workflow when claims have stamdata deviations. */
public interface HoeringService {

  HoeringEntity createHoering(UUID debtId, String deviationDescription);

  /** Creditor approves the hearing with a justification. */
  HoeringEntity creditorApprove(UUID hoeringId, String justification);

  /** Creditor withdraws the claim during hearing. */
  HoeringEntity creditorWithdraw(UUID hoeringId);

  /** RIM caseworker decides on the hearing. */
  HoeringEntity rimDecide(UUID hoeringId, boolean accepted, String notes);
}
