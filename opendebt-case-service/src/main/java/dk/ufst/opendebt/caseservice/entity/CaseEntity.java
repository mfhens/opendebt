package dk.ufst.opendebt.caseservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;

import lombok.*;

/**
 * OIO Sag-aligned case entity for debt collection case management.
 *
 * <p>Deprecated columns from the previous schema are kept for backward compatibility but are no
 * longer populated by the application. See V3 migration for details.
 */
@Entity
@Table(
    name = "cases",
    indexes = {
      @Index(name = "idx_case_case_state", columnList = "case_state"),
      @Index(name = "idx_case_case_type", columnList = "case_type"),
      @Index(name = "idx_case_primary_caseworker", columnList = "primary_caseworker_id"),
      @Index(name = "idx_case_parent_case_id", columnList = "parent_case_id"),
      @Index(name = "idx_cases_sensitivity", columnList = "sensitivity")
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

  // ── OIO Sag core fields ──────────────────────────────────────────────

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "confidential_title", length = 200)
  private String confidentialTitle;

  @Enumerated(EnumType.STRING)
  @Column(name = "case_state", nullable = false, length = 30)
  private CaseState caseState;

  @Column(name = "state_changed_at")
  private LocalDateTime stateChangedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "case_type", nullable = false, length = 30)
  private CaseType caseType;

  @Column(name = "subject_classification", length = 30)
  private String subjectClassification;

  @Column(name = "action_classification", length = 30)
  private String actionClassification;

  @Column(name = "precedent_indicator", nullable = false)
  @Builder.Default
  private boolean precedentIndicator = false;

  @Column(name = "retention_override")
  private Boolean retentionOverride;

  // ── Organisation / ownership ─────────────────────────────────────────

  @Column(name = "owner_organisation_id", length = 100)
  private String ownerOrganisationId;

  @Column(name = "responsible_unit_id", length = 100)
  private String responsibleUnitId;

  @Column(name = "primary_caseworker_id", length = 100)
  private String primaryCaseworkerId;

  // ── Access Control ───────────────────────────────────────────────────

  /**
   * Case sensitivity classification for access control (Petition048 W9-RBAC-02).
   *
   * <p>Determines which caseworkers can access the case based on their capabilities:
   *
   * <ul>
   *   <li>NORMAL: Any assigned caseworker can access
   *   <li>VIP: Requires HANDLE_VIP_CASES capability
   *   <li>PEP: Requires HANDLE_PEP_CASES capability
   *   <li>CONFIDENTIAL: Supervisors and admins only
   * </ul>
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "sensitivity", nullable = false, length = 20)
  @Builder.Default
  private CaseSensitivity sensitivity = CaseSensitivity.NORMAL;

  // ── Hierarchy / workflow ─────────────────────────────────────────────

  @Column(name = "parent_case_id")
  private UUID parentCaseId;

  @Column(name = "workflow_process_instance_id", length = 100)
  private String workflowProcessInstanceId;

  // ── Metadata ─────────────────────────────────────────────────────────

  @CurrentTimestamp(event = EventType.INSERT, source = SourceType.VM)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @CurrentTimestamp(source = SourceType.VM)
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @Version private Long version;

  // ── Deprecated columns(kept for V3 migration backward compat) ──────
  // These columns still exist in the database but are no longer used by the
  // application.  They will be dropped in a future migration.

  /**
   * @deprecated Moved to {@link CasePartyEntity} with role PRIMARY_DEBTOR.
   */
  @Deprecated(since = "v2", forRemoval = true)
  @Column(name = "debtor_person_id", insertable = false, updatable = false)
  private UUID debtorPersonId;

  /**
   * @deprecated Replaced by {@link #caseState}.
   */
  @Deprecated(since = "v2", forRemoval = true)
  @Column(name = "status", insertable = false, updatable = false, length = 30)
  private String status;

  /**
   * @deprecated Computed on demand from debt-service.
   */
  @Deprecated(since = "v2", forRemoval = true)
  @Column(name = "total_debt", insertable = false, updatable = false, precision = 15, scale = 2)
  private BigDecimal totalDebt;

  /**
   * @deprecated Computed on demand from debt-service.
   */
  @Deprecated(since = "v2", forRemoval = true)
  @Column(name = "total_paid", insertable = false, updatable = false, precision = 15, scale = 2)
  private BigDecimal totalPaid;

  /**
   * @deprecated Computed on demand from debt-service.
   */
  @Deprecated(since = "v2", forRemoval = true)
  @Column(
      name = "total_remaining",
      insertable = false,
      updatable = false,
      precision = 15,
      scale = 2)
  private BigDecimal totalRemaining;

  /**
   * @deprecated Replaced by {@link CollectionMeasureEntity}.
   */
  @Deprecated(since = "v2", forRemoval = true)
  @Column(name = "active_strategy", insertable = false, updatable = false, length = 30)
  private String activeStrategy;

  /**
   * @deprecated Replaced by {@link #primaryCaseworkerId}.
   */
  @Deprecated(since = "v2", forRemoval = true)
  @Column(name = "assigned_caseworker_id", insertable = false, updatable = false, length = 100)
  private String assignedCaseworkerId;

  /**
   * @deprecated Replaced by {@link CaseJournalNoteEntity}.
   */
  @Deprecated(since = "v2", forRemoval = true)
  @Column(name = "notes", insertable = false, updatable = false, columnDefinition = "TEXT")
  private String notes;

  /**
   * @deprecated Replaced by {@link CaseEventEntity}.
   */
  @Deprecated(since = "v2", forRemoval = true)
  @Column(name = "last_activity_at", insertable = false, updatable = false)
  private LocalDateTime lastActivityAt;

  /**
   * @deprecated Replaced by {@link CaseDebtEntity}.
   */
  @Deprecated(since = "v2", forRemoval = true)
  @ElementCollection
  @CollectionTable(name = "case_debt_ids", joinColumns = @JoinColumn(name = "case_id"))
  @Column(name = "debt_id")
  @Builder.Default
  private List<UUID> debtIds = new ArrayList<>();

  // ── Legacy enum types kept for reference (no longer used on entity) ──

  /**
   * @deprecated Use {@link CaseState} instead.
   */
  @Deprecated(since = "v2", forRemoval = true)
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

  /**
   * @deprecated Use {@link MeasureType} instead.
   */
  @Deprecated(since = "v2", forRemoval = true)
  public enum CollectionStrategy {
    VOLUNTARY_PAYMENT,
    PAYMENT_PLAN,
    WAGE_GARNISHMENT,
    OFFSETTING,
    LEGAL_ACTION
  }
}
