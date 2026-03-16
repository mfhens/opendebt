package dk.ufst.opendebt.debtservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "overdragelse_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverdragelseEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Column(name = "fordringshaver_id", nullable = false)
  private UUID fordringshaverId;

  @Column(name = "modtager_id")
  private UUID modtagerId;

  @Column(name = "tidspunkt", nullable = false)
  private LocalDateTime tidspunkt;

  @Column(name = "previous_state", length = 20)
  private String previousState;

  @Column(name = "new_state", length = 20)
  private String newState;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) createdAt = LocalDateTime.now();
    if (tidspunkt == null) tidspunkt = LocalDateTime.now();
  }
}
