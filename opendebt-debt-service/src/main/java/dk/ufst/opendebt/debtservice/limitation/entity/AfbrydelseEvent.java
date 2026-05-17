package dk.ufst.opendebt.debtservice.limitation.entity;

import java.time.LocalDate;
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
    name = "afbrydelse_event",
    indexes = @Index(name = "idx_afbrydelse_event_fordring_id", columnList = "fordring_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AfbrydelseEvent extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "fordring_id", nullable = false)
  private UUID fordringId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 40)
  private AfbrydelsesType type;

  @Column(name = "event_date", nullable = false)
  private LocalDate eventDate;

  @Column(name = "legal_reference", nullable = false)
  private String legalReference;

  @Column(name = "new_frist_expires", nullable = false)
  private LocalDate newFristExpires;

  @Column(name = "source_fordring_id")
  private UUID sourceFordringId;

  @Column(name = "target_fordring_id")
  private UUID targetFordringId;

  @Column(name = "propagation_reason")
  private String propagationReason;
}
