package dk.ufst.opendebt.citizen.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CitizenDebtSummaryResponse(
    List<CitizenDebtItem> debts,
    BigDecimal totalOutstandingAmount,
    int totalDebtCount,
    int pageNumber,
    int pageSize,
    int totalPages,
    long totalElements,
    List<CitizenEffectiveInterestRate> effectiveInterestRates) {

  public CitizenDebtSummaryResponse {
    debts = debts == null ? List.of() : List.copyOf(debts);
    effectiveInterestRates =
        effectiveInterestRates == null ? List.of() : List.copyOf(effectiveInterestRates);
  }
}
