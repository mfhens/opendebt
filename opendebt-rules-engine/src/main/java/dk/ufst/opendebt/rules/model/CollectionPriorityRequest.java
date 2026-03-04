package dk.ufst.opendebt.rules.model;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** Request to determine collection priority for offsetting/garnishment. */
@Data
@Builder
public class CollectionPriorityRequest {

  private UUID debtId;
  private String debtTypeCode;
  private String debtCategory;
  private BigDecimal totalAmount;
  private int daysPastDue;

  private boolean isChildSupport;
  private boolean isTaxDebt;
  private boolean isFine;
  private boolean isCourtOrdered;
}
