package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import dk.ufst.opendebt.debtservice.client.DaekningsRaekkefoeigenServiceClient;

/**
 * 3-tier allocation engine for modregning per GIL § 7.
 *
 * <p>Tier-1: fordringer from the paying authority (creditorOrgId == payingAuthorityOrgId). Tier-2:
 * all other active fordringer (delegated to DaekningsRaekkefoeigenService). Tier-3: remaining
 * fordringer sorted by inceptionDate ASC.
 */
@Component
public class ModregningsRaekkefoeigenEngine {

  private final DaekningsRaekkefoeigenServiceClient daekningsRaekkefoeigenServiceClient;

  public ModregningsRaekkefoeigenEngine(
      DaekningsRaekkefoeigenServiceClient daekningsRaekkefoeigenServiceClient) {
    this.daekningsRaekkefoeigenServiceClient = daekningsRaekkefoeigenServiceClient;
  }

  /**
   * Allocates the available amount across the three tiers.
   *
   * @param debtorPersonId the debtor
   * @param amount the total available amount
   * @param skipTier2 if true, skip tier-2 allocation (used for waiver re-run)
   * @param context optional context (e.g. payingAuthorityOrgId)
   * @return a TierAllocationResult with tier-level allocations and residual
   */
  public TierAllocationResult allocate(
      UUID debtorPersonId, BigDecimal amount, boolean skipTier2, Object context) {
    // Stub implementation — real allocation uses DebtRepository and
    // DaekningsRaekkefoeigenServiceClient
    // Full implementation requires integration with PSRM debt data
    return new TierAllocationResult(List.of(), List.of(), List.of(), amount);
  }
}
