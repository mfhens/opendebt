package dk.ufst.opendebt.debtservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

@Entity
@Table(
    name = "hoering",
    indexes = {
      @Index(name = "idx_hoering_debt_id", columnList = "debt_id"),
      @Index(name = "idx_hoering_status", columnList = "hoering_status"),
      @Index(name = "idx_hoering_sla_deadline", columnList = "sla_deadline")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoeringEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Enumerated(EnumType.STRING)
  @Column(name = "hoering_status", nullable = false, length = 30)
  private HoeringStatus hoeringStatus;

  /** Description of which stamdata deviated. */
  @Column(name = "deviation_description", nullable = false, length = 500)
  private String deviationDescription;

  /** Fordringshaver justification if approved. */
  @Column(name = "fordringshaver_begrundelse", length = 1000)
  private String fordingshaverBegrundelse;

  /** RIM caseworker decision notes. */
  @Column(name = "rim_decision", length = 500)
  private String rimDecision;

  /** SLA deadline — 14 days from creation. */
  @Column(name = "sla_deadline", nullable = false)
  private LocalDateTime slaDeadline;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;
}
