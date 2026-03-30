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

  public List<FordringProjection> getActiveFordringer(
      UUID debtorPersonId, int tier, UUID payingAuthorityOrgId) {
    return debtRepository.findActiveFordringerByTier(debtorPersonId, tier).stream()
        .map(
            d ->
                new FordringProjection(
                    d.getId(),
                    d.getOutstandingBalance() != null ? d.getOutstandingBalance() : BigDecimal.ZERO,
                    d.getInceptionDate()))
        .toList();
  }
}
