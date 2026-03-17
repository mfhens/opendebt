package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

/**
 * DTO representing a single write-up (opskrivning) on a claim. Includes all fields specified by
 * petition 030.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WriteUpDto {

  private String actionId;
  private String referenceActionId;
  private String formType;
  private String reason;
  private BigDecimal amount;
  private LocalDate effectiveDate;
  private String debtorId;
  private boolean annulled;
}
