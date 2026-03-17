package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

/**
 * DTO representing a single write-down (nedskrivning) on a claim. Includes all fields specified by
 * petition 030.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WriteDownDto {

  private String actionId;
  private String referenceActionId;
  private String formType;
  private String reasonCode;
  private BigDecimal amount;
  private LocalDate effectiveDate;
  private String debtorId;
}
