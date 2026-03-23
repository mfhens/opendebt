package dk.ufst.opendebt.creditor.dto;

import java.time.LocalDate;

import lombok.*;

/** Filter parameters for reconciliation list queries. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationFilterParams {

  private String status;
  private LocalDate periodEndFrom;
  private LocalDate periodEndTo;
  private LocalDate reconciliationStartFrom;
  private LocalDate reconciliationStartTo;
  private LocalDate reconciliationEndFrom;
  private LocalDate reconciliationEndTo;
}
