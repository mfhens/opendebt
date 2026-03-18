package dk.ufst.opendebt.caseservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;

/** Immutable event record for a case, providing a full audit trail. */
@Entity
@Table(
    name = "case_events",
    indexes = {
      @Index(name = "idx_case_events_case_performed", columnList = "case_id, performed_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseEventEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "case_id", nullable = false)
  private UUID caseId;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 40)
  private CaseEventType eventType;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "metadata", columnDefinition = "TEXT")
  private String metadata;

  @Column(name = "performed_by", length = 100)
  private String performedBy;

  @Column(name = "performed_at", nullable = false)
  private LocalDateTime performedAt;
}
