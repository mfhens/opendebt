package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;

import lombok.*;

/** DTO representing the basis data for a reconciliation period. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationBasisDto {

  @Builder.Default private BigDecimal influxAmount = BigDecimal.ZERO;

  @Builder.Default private BigDecimal recallAmount = BigDecimal.ZERO;

  @Builder.Default private BigDecimal writeUpAmount = BigDecimal.ZERO;

  @Builder.Default private BigDecimal writeDownAmount = BigDecimal.ZERO;
}
