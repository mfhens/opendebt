package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalCaseDto {

  private UUID id;
  private String caseNumber;
  private String status;
  private BigDecimal totalDebt;
  private BigDecimal totalRemaining;
}
