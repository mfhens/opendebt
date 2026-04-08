package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import dk.ufst.opendebt.debtservice.repository.DebtRepository;

import lombok.RequiredArgsConstructor;

/** Port for querying active fordringer by tier for the three-tier modregning engine (P058). */
@Component
@RequiredArgsConstructor
public class FordringQueryPort {

  private final DebtRepository debtRepository;

  /**
   * Returns active fordringer for {@code debtorPersonId} at the given {@code tier}.
   *
   * <p>For tier-1, when {@code payingAuthorityOrgId} is non-null, only fordringer whose {@code
   * creditorOrgId} matches the paying authority are returned (GIL § 7, stk. 1, nr. 1 — BUG-F /
   * P058).
   */
  public List<FordringProjection> getActiveFordringer(
      UUID debtorPersonId, int tier, UUID payingAuthorityOrgId) {
    var rawList =
        (tier == 1 && payingAuthorityOrgId != null)
            ? debtRepository.findActiveFordringerByTierAndCreditor(
                debtorPersonId, tier, payingAuthorityOrgId)
            : debtRepository.findActiveFordringerByTier(debtorPersonId, tier);
    return rawList.stream()
        .map(
            d ->
                new FordringProjection(
                    d.getId(),
                    d.getOutstandingBalance() != null ? d.getOutstandingBalance() : BigDecimal.ZERO,
                    d.getInceptionDate()))
        .toList();
  }

  /**
   * Returns the current outstanding balance for a specific fordring. Returns {@link
   * BigDecimal#ZERO} when the fordring does not exist or has no balance recorded.
   *
   * <p>Used by {@code KorrektionspuljeService} Step 1 to apply surplus to the reversed fordring's
   * uncovered renter before gendækning (Gæld.bekendtg. § 7, stk. 4 — BUG-A / P058).
   */
  public BigDecimal getOutstandingAmount(UUID fordringId) {
    return debtRepository
        .findById(fordringId)
        .map(d -> d.getOutstandingBalance() != null ? d.getOutstandingBalance() : BigDecimal.ZERO)
        .orElse(BigDecimal.ZERO);
  }
}
