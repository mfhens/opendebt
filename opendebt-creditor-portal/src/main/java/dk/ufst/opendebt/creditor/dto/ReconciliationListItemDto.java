package dk.ufst.opendebt.creditor.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.*;

/** DTO representing a single reconciliation period row in the reconciliation list. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationListItemDto {

  private UUID id;
  private String status;
  private int year;
  private int month;
  private LocalDate periodEndDate;
  private LocalDate reconciliationStartDate;
  private LocalDate reconciliationEndDate;
  private boolean responseSubmitted;
}
