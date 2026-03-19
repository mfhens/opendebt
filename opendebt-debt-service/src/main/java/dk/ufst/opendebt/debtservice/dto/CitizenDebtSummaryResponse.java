package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/** Citizen-facing debt summary response. Contains NO PII. */
@Data
@Builder
public class CitizenDebtSummaryResponse {

  private List<CitizenDebtItemDto> debts;
  private BigDecimal totalOutstandingAmount;
  private int totalDebtCount;
  private int pageNumber;
  private int pageSize;
  private int totalPages;
  private long totalElements;
}
