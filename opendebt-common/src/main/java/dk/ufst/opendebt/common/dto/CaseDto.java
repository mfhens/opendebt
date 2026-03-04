package dk.ufst.opendebt.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaseDto {

  private UUID id;

  private String caseNumber;

  private String debtorId;

  private String debtorName;

  private CaseStatus status;

  private BigDecimal totalDebt;

  private BigDecimal totalPaid;

  private BigDecimal totalRemaining;

  private List<UUID> debtIds;

  private CollectionStrategy activeStrategy;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  private String assignedCaseworkerId;

  private LocalDateTime lastActivityAt;

  private String notes;

  public enum CaseStatus {
    OPEN,
    IN_PROGRESS,
    AWAITING_PAYMENT,
    PAYMENT_PLAN_ACTIVE,
    WAGE_GARNISHMENT_ACTIVE,
    OFFSETTING_PENDING,
    UNDER_APPEAL,
    CLOSED_PAID,
    CLOSED_WRITTEN_OFF,
    CLOSED_CANCELLED
  }

  public enum CollectionStrategy {
    VOLUNTARY_PAYMENT,
    PAYMENT_PLAN,
    WAGE_GARNISHMENT,
    OFFSETTING,
    LEGAL_ACTION
  }
}
