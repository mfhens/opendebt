package dk.ufst.opendebt.debtservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "claim_lifecycle_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimLifecycleEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Column(name = "creditor_id", nullable = false)
  private UUID creditorId;

  @Column(name = "recipient_id")
  private UUID recipientId;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  @Column(name = "previous_state", length = 20)
  private String previousState;

  @Column(name = "new_state", length = 20)
  private String newState;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) createdAt = LocalDateTime.now();
    if (occurredAt == null) occurredAt = LocalDateTime.now();
  }
}
