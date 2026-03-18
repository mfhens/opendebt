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

  // ── New OIO Sag fields ───────────────────────────────────────────────

  private String title;

  private CaseState caseState;

  private LocalDateTime stateChangedAt;

  private String caseType;

  private String description;

  private String confidentialTitle;

  private String subjectClassification;

  private String actionClassification;

  private boolean precedentIndicator;

  private Boolean retentionOverride;

  private String ownerOrganisationId;

  private String responsibleUnitId;

  private String primaryCaseworkerId;

  private UUID parentCaseId;

  private String workflowProcessInstanceId;

  private List<CasePartyDto> parties;

  private int openObjections;

  // ── Backward-compatible fields (derived from new model) ──────────────

  /**
   * @deprecated Derived from PRIMARY_DEBTOR party.
   */
  @Deprecated private String debtorId;

  /**
   * @deprecated No longer stored; resolved via person-registry.
   */
  @Deprecated private String debtorName;

  /**
   * @deprecated Mapped from {@link #caseState}. Use caseState instead.
   */
  @Deprecated private CaseStatus status;

  /**
   * @deprecated Computed on demand from debt-service.
   */
  @Deprecated private BigDecimal totalDebt;

  /**
   * @deprecated Computed on demand from debt-service.
   */
  @Deprecated private BigDecimal totalPaid;

  /**
   * @deprecated Computed on demand from debt-service.
   */
  @Deprecated private BigDecimal totalRemaining;

  /**
   * @deprecated Derived from CaseDebtEntity.
   */
  @Deprecated private List<UUID> debtIds;

  /**
   * @deprecated Replaced by CollectionMeasureDto.
   */
  @Deprecated private CollectionStrategy activeStrategy;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  /**
   * @deprecated Use {@link #primaryCaseworkerId}.
   */
  @Deprecated private String assignedCaseworkerId;

  /**
   * @deprecated Replaced by CaseEventEntity.
   */
  @Deprecated private LocalDateTime lastActivityAt;

  /**
   * @deprecated Replaced by CaseJournalNoteDto.
   */
  @Deprecated private String notes;

  /** OIO Sag-aligned case lifecycle states. */
  public enum CaseState {
    CREATED,
    ASSESSED,
    DECIDED,
    SUSPENDED,
    CLOSED_PAID,
    CLOSED_WRITTEN_OFF,
    CLOSED_WITHDRAWN,
    CLOSED_CANCELLED
  }

  /**
   * @deprecated Use {@link CaseState} instead.
   */
  @Deprecated
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
   * @deprecated Use CollectionMeasureDto instead.
   */
  @Deprecated
  public enum CollectionStrategy {
    VOLUNTARY_PAYMENT,
    PAYMENT_PLAN,
    WAGE_GARNISHMENT,
    OFFSETTING,
    LEGAL_ACTION
  }
}
