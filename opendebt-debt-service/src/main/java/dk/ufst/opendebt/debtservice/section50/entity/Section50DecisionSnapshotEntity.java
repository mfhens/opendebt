package dk.ufst.opendebt.debtservice.section50.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import dk.ufst.opendebt.common.audit.AuditableEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "section50_decision_snapshot",
    indexes = {
      @Index(name = "idx_s50_snapshot_worklist", columnList = "worklist_id", unique = true),
      @Index(name = "idx_s50_snapshot_rule_path", columnList = "rule_path")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section50DecisionSnapshotEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "worklist_id", nullable = false, unique = true)
  private UUID worklistId;

  @Column(name = "rule_path", nullable = false, length = 100)
  private String rulePath;

  @Column(name = "input_hash", nullable = false, length = 128)
  private String inputHash;

  @Column(name = "selected_next_item_id", length = 100)
  private String selectedNextItemId;

  @Column(name = "legal_reference", nullable = false, length = 200)
  private String legalReference;

  @Column(name = "audit_event_id", nullable = false)
  private UUID auditEventId;

  @Column(name = "origin", nullable = false, length = 100)
  private String origin;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "notes", length = 1000)
  private String notes;

  @Column(name = "prioritisation_factors", length = 1000)
  private String prioritisationFactors;
}
