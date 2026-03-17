package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import lombok.*;

/** DTO for submitting a reconciliation response. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationResponseDto {

  @NotNull private BigDecimal explainedDifference;

  @NotNull private BigDecimal unexplainedDifference;

  @NotNull private BigDecimal totalDifference;
}
