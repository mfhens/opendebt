package dk.ufst.opendebt.caseservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

@Entity
@Table(
    name = "cases",
    indexes = {
      @Index(name = "idx_case_debtor_person_id", columnList = "debtor_person_id"),
      @Index(name = "idx_case_status", columnList = "status"),
      @Index(name = "idx_case_caseworker", columnList = "assigned_caseworker_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "case_number", unique = true, nullable = false, length = 20)
  private String caseNumber;

  /** Reference to person-registry.persons - NO PII stored here */
  @Column(name = "debtor_person_id", nullable = false)
  private UUID debtorPersonId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private CaseStatus status;

  @Column(name = "total_debt", precision = 15, scale = 2)
  private BigDecimal totalDebt;

  @Column(name = "total_paid", precision = 15, scale = 2)
  private BigDecimal totalPaid;

  @Column(name = "total_remaining", precision = 15, scale = 2)
  private BigDecimal totalRemaining;

  @ElementCollection
  @CollectionTable(name = "case_debt_ids", joinColumns = @JoinColumn(name = "case_id"))
  @Column(name = "debt_id")
  @Builder.Default
  private List<UUID> debtIds = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "active_strategy", length = 30)
  private CollectionStrategy activeStrategy;

  @Column(name = "assigned_caseworker_id", length = 100)
  private String assignedCaseworkerId;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "last_activity_at")
  private LocalDateTime lastActivityAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @Version private Long version;

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
