package dk.ufst.opendebt.debtservice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;

import lombok.*;

@Entity
@Table(
    name = "debts",
    indexes = {
      @Index(name = "idx_debt_debtor_person_id", columnList = "debtor_person_id"),
      @Index(name = "idx_debt_creditor_org_id", columnList = "creditor_org_id"),
      @Index(name = "idx_debt_status", columnList = "status"),
      @Index(name = "idx_debt_readiness", columnList = "readiness_status"),
      @Index(name = "idx_debt_due_date", columnList = "due_date"),
      @Index(name = "idx_debt_parent_claim_id", columnList = "parent_claim_id"),
      @Index(name = "idx_debt_lifecycle_state", columnList = "lifecycle_state")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** Reference to person-registry.persons - NO PII stored here */
  @Column(name = "debtor_person_id", nullable = false)
  private UUID debtorPersonId;

  /** Reference to person-registry.organizations - NO PII stored here */
  @Column(name = "creditor_org_id", nullable = false)
  private UUID creditorOrgId;

  @Column(name = "debt_type_code", nullable = false, length = 20)
  private String debtTypeCode;

  @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal principalAmount;

  @Column(name = "interest_amount", precision = 15, scale = 2)
  private BigDecimal interestAmount;

  @Column(name = "fees_amount", precision = 15, scale = 2)
  private BigDecimal feesAmount;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "original_due_date")
  private LocalDate originalDueDate;

  @Column(name = "external_reference", length = 100)
  private String externalReference;

  // AIDEV-NOTE: OCR-linje is the primary key for auto-matching incoming CREMUL payments
  // (petition001).
  // It must be unique across active debts; uniqueness is not DB-enforced yet — consider adding
  // a partial unique index (WHERE status NOT IN ('PAID','CANCELLED','WRITTEN_OFF')).
  // AIDEV-TODO: Add partial unique index on ocr_line for non-terminal debts via Flyway migration.
  /** Betalingsservice OCR-linje for automatic payment matching. */
  @Column(name = "ocr_line", length = 50)
  private String ocrLine;

  /** Remaining balance after payments (write-downs). */
  @Column(name = "outstanding_balance", precision = 15, scale = 2)
  private BigDecimal outstandingBalance;

  // --- PSRM Master Data Fields (W7-STAM-01) ---

  @Column(name = "principal", precision = 15, scale = 2)
  private BigDecimal principal;

  @Column(name = "creditor_reference", length = 50)
  private String creditorReference;

  @Enumerated(EnumType.STRING)
  @Column(name = "claim_art", length = 10)
  private ClaimArtEnum claimArt;

  @Enumerated(EnumType.STRING)
  @Column(name = "claim_category", length = 5)
  private ClaimCategory claimCategory;

  @Column(name = "parent_claim_id")
  private UUID parentClaimId;

  @Column(name = "limitation_date")
  private LocalDate limitationDate;

  @Column(name = "description", length = 100)
  private String description;

  @Column(name = "period_from")
  private LocalDate periodFrom;

  @Column(name = "period_to")
  private LocalDate periodTo;

  @Column(name = "inception_date")
  private LocalDate inceptionDate;

  @Column(name = "payment_deadline")
  private LocalDate paymentDeadline;

  @Column(name = "last_payment_date")
  private LocalDate lastPaymentDate;

  @Column(name = "estate_processing")
  private Boolean estateProcessing;

  @Column(name = "judgment_date")
  private LocalDate judgmentDate;

  @Column(name = "settlement_date")
  private LocalDate settlementDate;

  @Embedded private InterestSelectionEmbeddable interestSelection;

  @Column(name = "claim_note", length = 500)
  private String claimNote;

  @Column(name = "customer_note", length = 500)
  private String customerNote;

  @Column(name = "p_number", length = 20)
  private String pNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "lifecycle_state", length = 20)
  private ClaimLifecycleState lifecycleState;

  @Column(name = "received_at")
  private LocalDateTime receivedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private DebtStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "readiness_status", nullable = false, length = 20)
  private ReadinessStatus readinessStatus;

  @Column(name = "readiness_rejection_reason", length = 500)
  private String readinessRejectionReason;

  @Column(name = "readiness_validated_at")
  private LocalDateTime readinessValidatedAt;

  @Column(name = "readiness_validated_by", length = 100)
  private String readinessValidatedBy;

  @CurrentTimestamp(event = EventType.INSERT, source = SourceType.VM)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @CurrentTimestamp(source = SourceType.VM)
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @Version private Long version;

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

  public BigDecimal getTotalAmount() {
    BigDecimal total = principalAmount != null ? principalAmount : BigDecimal.ZERO;
    if (interestAmount != null) {
      total = total.add(interestAmount);
    }
    if (feesAmount != null) {
      total = total.add(feesAmount);
    }
    return total;
  }
}
