package dk.ufst.rules.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** Request to evaluate if a debt is ready for collection (indrivelsesparat). */
@Data
@Builder
public class DebtReadinessRequest {

  private UUID debtId;
  private UUID debtorPersonId;
  private UUID creditorOrgId;

  private String debtTypeCode;
  private BigDecimal principalAmount;
  private BigDecimal interestAmount;
  private BigDecimal feesAmount;
  private LocalDate dueDate;
  private LocalDate originalDueDate;

  private boolean debtorIdentified;
  private boolean documentationComplete;
  private boolean activeDispute;
  private boolean underAppeal;

  private int daysPastDue;
  private int previousCollectionAttempts;
}
