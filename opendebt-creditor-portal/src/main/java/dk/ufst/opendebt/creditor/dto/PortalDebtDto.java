package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalDebtDto {

  private UUID id;
  private UUID debtorPersonId;
  private UUID creditorOrgId;
  private BigDecimal principalAmount;
  private BigDecimal outstandingBalance;
  private String status;
  private LocalDate dueDate;
  private String debtTypeCode;
  private String description;
}
