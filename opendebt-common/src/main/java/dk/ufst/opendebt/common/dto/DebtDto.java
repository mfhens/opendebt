package dk.ufst.opendebt.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DebtDto {

  private UUID id;

  @NotBlank(message = "Debtor CPR/CVR is required")
  private String debtorId;

  @NotBlank(message = "Creditor ID is required")
  private String creditorId;

  @NotBlank(message = "Debt type code is required")
  private String debtTypeCode;

  @NotNull(message = "Principal amount is required")
  @Positive(message = "Principal amount must be positive")
  private BigDecimal principalAmount;

  private BigDecimal interestAmount;

  private BigDecimal feesAmount;

  @NotNull(message = "Due date is required")
  private LocalDate dueDate;

  private LocalDate originalDueDate;

  private String externalReference;

  private DebtStatus status;

  private ReadinessStatus readinessStatus;

  private String readinessRejectionReason;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  private String createdBy;

  public enum DebtStatus {
    PENDING,
    ACTIVE,
    IN_COLLECTION,
    PARTIALLY_PAID,
    PAID,
    WRITTEN_OFF,
    DISPUTED,
    CANCELLED
  }

  public enum ReadinessStatus {
    PENDING_REVIEW,
    READY_FOR_COLLECTION,
    NOT_READY,
    UNDER_APPEAL
  }
}
