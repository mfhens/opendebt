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

  /** Carries both the allocations made and the remaining (unallocated) amount from a tier step. */
  private record AllocationStep(List<FordringAllocation> allocations, BigDecimal remaining) {}

  /**
   * Allocates the available amount across the three tiers.
   *
   * @param debtorPersonId the debtor
   * @param amount the total available amount
   * @param skipTier2 if true, skip tier-2 allocation (used for waiver re-run)
   * @param payingAuthorityOrgId filters tier-1 to fordringer from this authority (GIL § 7, stk. 1,
   *     nr. 1); may be null
   * @return a TierAllocationResult with tier-level allocations and residual
   */
  public TierAllocationResult allocate(
      UUID debtorPersonId, BigDecimal amount, boolean skipTier2, UUID payingAuthorityOrgId) {

    AllocationStep step1 =
        allocateSimpleTier(
            fordringQueryPort.getActiveFordringer(debtorPersonId, 1, payingAuthorityOrgId),
            amount,
            1);
    if (step1.remaining().compareTo(BigDecimal.ZERO) <= 0) {
      return new TierAllocationResult(step1.allocations(), List.of(), List.of(), BigDecimal.ZERO);
    }

    AllocationStep step2 =
        skipTier2
            ? new AllocationStep(List.of(), step1.remaining())
            : allocateTier2(debtorPersonId, step1.remaining());
    if (step2.remaining().compareTo(BigDecimal.ZERO) <= 0) {
      return new TierAllocationResult(
          step1.allocations(), step2.allocations(), List.of(), BigDecimal.ZERO);
    }

    AllocationStep step3 =
        allocateSimpleTier(
            fordringQueryPort.getActiveFordringer(debtorPersonId, 3, null), step2.remaining(), 3);
    return new TierAllocationResult(
        step1.allocations(),
        step2.allocations(),
        step3.allocations(),
        step3.remaining().setScale(2, RoundingMode.HALF_UP));
  }

  // ── private helpers ──────────────────────────────────────────────────────────

  /**
   * Greedy linear allocation for a single tier (tiers 1 and 3 follow the same pattern). Iterates
   * {@code fordringer} in order, covering as much of each as {@code remaining} allows.
   */
  private AllocationStep allocateSimpleTier(
      List<FordringProjection> fordringer, BigDecimal remaining, int tier) {
    List<FordringAllocation> allocations = new ArrayList<>();
    for (FordringProjection f : fordringer) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
      BigDecimal covered =
          remaining.min(f.tilbaestaaendeBeloeb()).setScale(2, RoundingMode.HALF_UP);
      if (covered.compareTo(BigDecimal.ZERO) > 0) {
        allocations.add(new FordringAllocation(f.fordringId(), covered, tier));
        remaining = remaining.subtract(covered);
      }
    }
    return new AllocationStep(allocations, remaining);
  }

  /**
   * Tier-2 allocation. If {@code remaining} covers the full tier-2 total, all fordringer are
   * allocated directly. Otherwise, delegates to the DaekningsRaekkefoeigenService (P057) with a
   * first-fordring fallback when P057 returns empty.
   */
  private AllocationStep allocateTier2(UUID debtorPersonId, BigDecimal remaining) {
    List<FordringProjection> fordringer =
        fordringQueryPort.getActiveFordringer(debtorPersonId, 2, null);
    BigDecimal tier2Total =
        fordringer.stream()
            .map(FordringProjection::tilbaestaaendeBeloeb)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (remaining.compareTo(tier2Total) >= 0) {
      return allocateTier2FullCoverage(fordringer, remaining, tier2Total);
    }
    return allocateTier2Partial(debtorPersonId, remaining, fordringer);
  }

  /** Full tier-2 coverage — allocate every fordring at face value. */
  private AllocationStep allocateTier2FullCoverage(
      List<FordringProjection> fordringer, BigDecimal remaining, BigDecimal tier2Total) {
    List<FordringAllocation> allocations = new ArrayList<>();
    for (FordringProjection f : fordringer) {
      if (f.tilbaestaaendeBeloeb().compareTo(BigDecimal.ZERO) > 0) {
        allocations.add(new FordringAllocation(f.fordringId(), f.tilbaestaaendeBeloeb(), 2));
      }
    }
    return new AllocationStep(allocations, remaining.subtract(tier2Total));
  }

  /**
   * Partial tier-2 coverage. Delegates to P057; if P057 returns empty, applies {@code remaining} to
   * the first fordring in registration order (GIL § 7 fallback).
   */
  private AllocationStep allocateTier2Partial(
      UUID debtorPersonId, BigDecimal remaining, List<FordringProjection> fordringer) {
    List<FordringAllocation> p057Result =
        daekningsRaekkefoeigenServiceClient.allocate(debtorPersonId, remaining);
    if (!p057Result.isEmpty()) {
      return new AllocationStep(p057Result, BigDecimal.ZERO);
    }
    if (fordringer.isEmpty()) {
      return new AllocationStep(List.of(), remaining);
    }
    FordringProjection first = fordringer.get(0);
    BigDecimal covered =
        remaining.min(first.tilbaestaaendeBeloeb()).setScale(2, RoundingMode.HALF_UP);
    if (covered.compareTo(BigDecimal.ZERO) <= 0) {
      return new AllocationStep(List.of(), remaining);
    }
    return new AllocationStep(
        List.of(new FordringAllocation(first.fordringId(), covered, 2)),
        remaining.subtract(covered));
  }
}
