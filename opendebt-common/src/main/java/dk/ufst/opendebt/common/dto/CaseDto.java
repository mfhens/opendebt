package dk.ufst.opendebt.common.dto;

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

  // ── OIO Sag fields ───────────────────────────────────────────────────

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
  // These bridge the v1→v2 model transition and are consumed by downstream
  // services (payment-service, caseworker-portal) until they migrate to the
  // party / case-debts APIs.

  /** Derived from the PRIMARY_DEBTOR {@link CasePartyDto} entry. */
  private String debtorId;

  /** Derived from active {@code case_debts} rows for this case. */
  private List<UUID> debtIds;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

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
}
