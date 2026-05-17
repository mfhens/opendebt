package dk.ufst.opendebt.caseservice.limitation;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import dk.ufst.opendebt.common.audit.AuditableEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "limitation_objection_workflow_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LimitationObjectionWorkflowRecord extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "fordring_id", nullable = false)
  private UUID fordringId;

  @Column(name = "debtor_person_id", nullable = false)
  private UUID debtorPersonId;

  @Column(name = "workflow_case_id", nullable = false)
  private UUID workflowCaseId;

  @Column(name = "status", nullable = false, length = 30)
  private String status;

  @Column(name = "registered_by", length = 100)
  private String registeredBy;

  @Column(name = "registered_at")
  private Instant registeredAt;

  @Column(name = "decided_by", length = 100)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "rationale", columnDefinition = "TEXT")
  private String rationale;
}
