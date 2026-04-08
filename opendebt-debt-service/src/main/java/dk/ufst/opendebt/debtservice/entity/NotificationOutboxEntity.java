package dk.ufst.opendebt.debtservice.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;

/** Transactional outbox entry for Digital Post notice dispatch — SPEC-058 §3.1 MISSING-1. */
@Entity
@Table(name = "notification_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationOutboxEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "modregning_event_id", nullable = false)
  private UUID modregningEventId;

  @Column(name = "debtor_person_id", nullable = false)
  private UUID debtorPersonId;

  @Column(name = "payload", nullable = false, columnDefinition = "text")
  private String payload;

  @Builder.Default
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Builder.Default
  @Column(name = "dispatched", nullable = false)
  private boolean dispatched = false;
}
