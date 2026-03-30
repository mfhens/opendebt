package dk.ufst.opendebt.debtservice.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import dk.ufst.opendebt.common.audit.AuditableEntity;

import lombok.*;

@Entity
@Table(
    name = "collection_measures",
    indexes = {
      @Index(name = "idx_measure_debt_id", columnList = "debt_id"),
      @Index(name = "idx_measure_type", columnList = "measure_type"),
      @Index(name = "idx_measure_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionMeasureEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Enumerated(EnumType.STRING)
  @Column(name = "measure_type", nullable = false, length = 30)
  private MeasureType measureType;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private MeasureStatus status = MeasureStatus.INITIATED;

  @Column(name = "initiated_by", length = 100)
  private String initiatedBy;

  @Builder.Default
  @Column(name = "initiated_at", nullable = false)
  private Instant initiatedAt = Instant.now();

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "amount", precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(name = "note", length = 500)
  private String note;

  @Column(name = "modregning_event_id")
  private UUID modregningEventId;

  @Builder.Default
  @Column(name = "waiver_applied", nullable = false)
  private boolean waiverApplied = false;

  @Column(name = "caseworker_id")
  private UUID caseworkerId;

  public enum MeasureType {
    SET_OFF,
    WAGE_GARNISHMENT,
    ATTACHMENT
  }

  public enum MeasureStatus {
    INITIATED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
  }
}
