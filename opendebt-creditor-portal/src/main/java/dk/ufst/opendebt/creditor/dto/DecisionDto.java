package dk.ufst.opendebt.creditor.dto;

import java.time.LocalDate;

import lombok.*;

/**
 * DTO representing a court decision (dom) or settlement (forlig) associated with a claim. Displayed
 * only for single-debtor claims.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionDto {

  private String type;
  private LocalDate date;
  private String description;
}
