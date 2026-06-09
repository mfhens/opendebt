package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Citizen-facing debt summary response. Contains NO PII. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CitizenDebtSummaryResponse {

  @Builder.Default private List<CitizenDebtItemDto> debts = List.of();
  private BigDecimal totalOutstandingAmount;
  private int totalDebtCount;
  private int pageNumber;
  private int pageSize;
  private int totalPages;
  private long totalElements;
  @Builder.Default private List<CitizenEffectiveInterestRateDto> effectiveInterestRates = List.of();
}
