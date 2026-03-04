package dk.ufst.opendebt.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentDto {

  private UUID id;

  @NotNull(message = "Case ID is required")
  private UUID caseId;

  private UUID debtId;

  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be positive")
  private BigDecimal amount;

  @NotBlank(message = "Payment method is required")
  private PaymentMethod paymentMethod;

  private PaymentStatus status;

  private String transactionReference;

  private LocalDateTime paymentDate;

  private LocalDateTime processedAt;

  private String processedBy;

  private String externalPaymentId;

  private String failureReason;

  public enum PaymentMethod {
    BANK_TRANSFER,
    CARD_PAYMENT,
    WAGE_GARNISHMENT,
    OFFSETTING,
    CASH,
    DIRECT_DEBIT
  }

  public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED
  }
}
