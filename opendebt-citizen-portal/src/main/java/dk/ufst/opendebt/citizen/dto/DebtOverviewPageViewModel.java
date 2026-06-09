package dk.ufst.opendebt.citizen.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DebtOverviewPageViewModel {

  private final String totalOutstandingAmount;
  private final int totalDebtCount;
  private final List<DebtOverviewRowViewModel> debts;
  private final List<String> effectiveInterestRateNotes;
  private final boolean noDebt;
  private final boolean serviceUnavailable;
  private final String paymentInfoUrl;
  private final String debtPdfUrl;
  private final String phoneNumber;
  private final String phoneInternational;
  private final String landingPageUrl;
  private final int currentPage;
  private final int totalPages;
  private final boolean hasPrevious;
  private final boolean hasNext;
  private final Integer previousPage;
  private final Integer nextPage;
}
