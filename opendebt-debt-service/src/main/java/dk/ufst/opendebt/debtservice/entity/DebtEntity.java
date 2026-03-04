package dk.ufst.opendebt.debtservice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

@Entity
@Table(
    name = "debts",
    indexes = {
      @Index(name = "idx_debt_debtor_person_id", columnList = "debtor_person_id"),
      @Index(name = "idx_debt_creditor_org_id", columnList = "creditor_org_id"),
      @Index(name = "idx_debt_status", columnList = "status"),
      @Index(name = "idx_debt_readiness", columnList = "readiness_status"),
      @Index(name = "idx_debt_due_date", columnList = "due_date")
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

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
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
