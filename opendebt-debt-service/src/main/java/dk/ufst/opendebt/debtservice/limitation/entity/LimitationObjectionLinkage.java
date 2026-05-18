package dk.ufst.opendebt.debtservice.limitation.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import dk.ufst.opendebt.common.audit.AuditableEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "limitation_objection_linkage",
    indexes =
        @Index(name = "idx_limitation_objection_linkage_fordring_id", columnList = "fordring_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LimitationObjectionLinkage extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "fordring_id", nullable = false)
  private UUID fordringId;

  @Column(name = "indsigelse_id", nullable = false, unique = true)
  private UUID indsigelsesId;

  @Column(name = "workflow_case_id", nullable = false)
  private UUID workflowCaseId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private ObjectionStatus status;

  @Column(name = "rationale", columnDefinition = "TEXT")
  private String rationale;
}
