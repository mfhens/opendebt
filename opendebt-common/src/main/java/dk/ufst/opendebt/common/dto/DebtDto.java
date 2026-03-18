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

  private String ocrLine;

  private BigDecimal outstandingBalance;

  private String claimArt;

  private String claimCategory;

  private String creditorReference;

  private String description;

  private LocalDate limitationDate;

  private LocalDate periodFrom;

  private LocalDate periodTo;

  private LocalDate inceptionDate;

  private LocalDate paymentDeadline;

  private LocalDate lastPaymentDate;

  private Boolean estateProcessing;

  private LocalDate judgmentDate;

  private LocalDate settlementDate;

  private String interestRule;

  private String interestRateCode;

  private BigDecimal additionalInterestRate;

  private String claimNote;

  private String customerNote;

  private String lifecycleState;

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
