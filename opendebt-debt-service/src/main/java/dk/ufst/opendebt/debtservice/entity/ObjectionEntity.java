package dk.ufst.opendebt.debtservice.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import dk.ufst.opendebt.common.audit.AuditableEntity;

import lombok.*;

@Entity
@Table(
    name = "objections",
    indexes = {
      @Index(name = "idx_objection_debt_id", columnList = "debt_id"),
      @Index(name = "idx_objection_debtor", columnList = "debtor_person_id"),
      @Index(name = "idx_objection_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectionEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Column(name = "debtor_person_id", nullable = false)
  private UUID debtorPersonId;

  @Column(name = "reason", nullable = false, length = 500)
  private String reason;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ObjectionStatus status = ObjectionStatus.ACTIVE;

  @Builder.Default
  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt = Instant.now();

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "resolution_note", length = 500)
  private String resolutionNote;

  public enum ObjectionStatus {
    ACTIVE,
    UPHELD,
    REJECTED
  }
}
