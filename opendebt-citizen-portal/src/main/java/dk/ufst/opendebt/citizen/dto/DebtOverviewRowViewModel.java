package dk.ufst.opendebt.citizen.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DebtOverviewRowViewModel {

  private final UUID debtId;
  private final String debtTypeName;
  private final String creditorDisplayName;
  private final String principalAmount;
  private final String outstandingAmount;
  private final String interestAmount;
  private final String dueDate;
  private final String statusLabel;
  private final String statusDetail;
  private final String interestDetail;
}
