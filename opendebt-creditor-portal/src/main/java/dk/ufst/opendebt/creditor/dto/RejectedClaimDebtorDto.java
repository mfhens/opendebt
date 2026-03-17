package dk.ufst.opendebt.creditor.dto;

import java.time.LocalDate;

import lombok.*;

/**
 * DTO representing debtor information for a rejected claim, including dates, flags, and notes
 * required by the rejected claim detail view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectedClaimDebtorDto {

  private String identifierType;
  private String identifier;
  private LocalDate dueDate;
  private LocalDate lastTimelyPaymentDate;
  private LocalDate limitationDate;
  private LocalDate courtDate;
  private LocalDate settlementDate;
  private boolean estateProcessing;
  private String debtorNote;
}
