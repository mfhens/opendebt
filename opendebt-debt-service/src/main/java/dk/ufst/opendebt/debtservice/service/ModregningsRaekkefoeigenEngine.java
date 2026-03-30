package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import dk.ufst.opendebt.debtservice.client.DaekningsRaekkefoeigenServiceClient;

import lombok.RequiredArgsConstructor;

/**
 * 3-tier allocation engine for modregning per GIL § 7.
 *
 * <p>Tier-1: fordringer from the paying authority (modregningTier=1). Tier-2: fordringer under RIM
 * inddrivelse (modregningTier=2), full coverage direct, partial via DaekningsRaekkefoeigenService.
 * Tier-3: other active fordringer (modregningTier=3), sorted by inceptionDate ASC.
 */
@Component
@RequiredArgsConstructor
public class ModregningsRaekkefoeigenEngine {

  private final DaekningsRaekkefoeigenServiceClient daekningsRaekkefoeigenServiceClient;
  private final FordringQueryPort fordringQueryPort;

  /**
   * Allocates the available amount across the three tiers.
   *
   * @param debtorPersonId the debtor
   * @param amount the total available amount
   * @param skipTier2 if true, skip tier-2 allocation (used for waiver re-run)
   * @param context optional context (unused; reserved for future use)
   * @return a TierAllocationResult with tier-level allocations and residual
   */
  public TierAllocationResult allocate(
      UUID debtorPersonId, BigDecimal amount, boolean skipTier2, Object context) {
    BigDecimal remaining = amount;
    List<FordringAllocation> tier1 = new ArrayList<>();
    List<FordringAllocation> tier2 = new ArrayList<>();
    List<FordringAllocation> tier3 = new ArrayList<>();

    // Tier 1: fordringer registered by the paying authority
    for (FordringProjection f : fordringQueryPort.getActiveFordringer(debtorPersonId, 1, null)) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
      BigDecimal covered =
          remaining.min(f.tilbaestaaendeBeloeb()).setScale(2, RoundingMode.HALF_UP);
      if (covered.compareTo(BigDecimal.ZERO) > 0) {
        tier1.add(new FordringAllocation(f.fordringId(), covered, 1));
        remaining = remaining.subtract(covered);
      }
    }

    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
      return new TierAllocationResult(tier1, tier2, tier3, BigDecimal.ZERO);
    }

    // Tier 2: fordringer under RIM inddrivelse
    if (!skipTier2) {
      List<FordringProjection> tier2Fordringer =
          fordringQueryPort.getActiveFordringer(debtorPersonId, 2, null);
      BigDecimal tier2Total =
          tier2Fordringer.stream()
              .map(FordringProjection::tilbaestaaendeBeloeb)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      if (remaining.compareTo(tier2Total) >= 0) {
        // Full coverage — no P057 call needed
        for (FordringProjection f : tier2Fordringer) {
          if (f.tilbaestaaendeBeloeb().compareTo(BigDecimal.ZERO) > 0) {
            tier2.add(new FordringAllocation(f.fordringId(), f.tilbaestaaendeBeloeb(), 2));
          }
        }
        remaining = remaining.subtract(tier2Total);
      } else {
        // Partial — delegate to DaekningsRaekkefoeigenService (P057)
        List<FordringAllocation> p057Result =
            daekningsRaekkefoeigenServiceClient.allocate(debtorPersonId, remaining);
        if (p057Result.isEmpty()) {
          // Fallback: apply to first fordring in registration order
          if (!tier2Fordringer.isEmpty()) {
            FordringProjection first = tier2Fordringer.get(0);
            BigDecimal covered =
                remaining.min(first.tilbaestaaendeBeloeb()).setScale(2, RoundingMode.HALF_UP);
            if (covered.compareTo(BigDecimal.ZERO) > 0) {
              tier2.add(new FordringAllocation(first.fordringId(), covered, 2));
              remaining = remaining.subtract(covered);
            }
          }
        } else {
          tier2.addAll(p057Result);
          remaining = BigDecimal.ZERO;
        }
      }
    }

    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
      return new TierAllocationResult(tier1, tier2, tier3, BigDecimal.ZERO);
    }

    // Tier 3: other active fordringer, sorted ascending by inceptionDate
    for (FordringProjection f : fordringQueryPort.getActiveFordringer(debtorPersonId, 3, null)) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
      BigDecimal covered =
          remaining.min(f.tilbaestaaendeBeloeb()).setScale(2, RoundingMode.HALF_UP);
      if (covered.compareTo(BigDecimal.ZERO) > 0) {
        tier3.add(new FordringAllocation(f.fordringId(), covered, 3));
        remaining = remaining.subtract(covered);
      }
    }

    return new TierAllocationResult(
        tier1, tier2, tier3, remaining.setScale(2, RoundingMode.HALF_UP));
  }
}
