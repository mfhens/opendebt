package dk.ufst.opendebt.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

@Entity
@Table(
    name = "payments",
    indexes = {
      @Index(name = "idx_payment_case_id", columnList = "case_id"),
      @Index(name = "idx_payment_debt_id", columnList = "debt_id"),
      @Index(name = "idx_payment_status", columnList = "status"),
      @Index(name = "idx_payment_date", columnList = "payment_date")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "case_id")
  private UUID caseId;

  @Column(name = "debt_id")
  private UUID debtId;

  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false, length = 20)
  private PaymentMethod paymentMethod;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private PaymentStatus status;

  @Column(name = "transaction_reference", length = 100)
  private String transactionReference;

  @Column(name = "external_payment_id", length = 100)
  private String externalPaymentId;

  /** Betalingsservice OCR-linje used for automatic matching. */
  @Column(name = "ocr_line", length = 50)
  private String ocrLine;

  @Column(name = "payment_date")
  private LocalDateTime paymentDate;

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  @Column(name = "processed_by", length = 100)
  private String processedBy;

  @Column(name = "failure_reason", length = 500)
  private String failureReason;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Version private Long version;

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
