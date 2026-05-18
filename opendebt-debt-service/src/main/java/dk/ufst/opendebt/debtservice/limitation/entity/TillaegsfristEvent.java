package dk.ufst.opendebt.debtservice.limitation.entity;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "tillaegsfrist_event",
    indexes = @Index(name = "idx_tillaegsfrist_event_fordring_id", columnList = "fordring_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TillaegsfristEvent extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "fordring_id", nullable = false)
  private UUID fordringId;

  @Column(name = "type", nullable = false, length = 50)
  private String type;

  @Column(name = "applied_date", nullable = false)
  private LocalDate appliedDate;

  @Column(name = "extension_years", nullable = false)
  private int extensionYears;

  @Column(name = "new_frist_expires", nullable = false)
  private LocalDate newFristExpires;

  @Column(name = "legal_reference", nullable = false)
  private String legalReference;
}
