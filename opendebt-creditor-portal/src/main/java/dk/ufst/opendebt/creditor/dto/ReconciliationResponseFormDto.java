package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import lombok.*;

/** Form-backing DTO for reconciliation response submission with tamper-protection fields. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationResponseFormDto {

  @NotNull(message = "{reconciliation.validation.explained.required}")
  private BigDecimal explainedDifference;

  @NotNull(message = "{reconciliation.validation.unexplained.required}")
  private BigDecimal unexplainedDifference;

  @NotNull(message = "{reconciliation.validation.total.required}")
  private BigDecimal totalDifference;

  /** SHA-256 hash of the basis data, used for tamper protection. */
  private String basisChecksum;

  /** Original basis data amounts for BFF verification. */
  private BigDecimal basisInflux;

  private BigDecimal basisRecall;
  private BigDecimal basisWriteUp;
  private BigDecimal basisWriteDown;
}
