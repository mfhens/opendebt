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
    name = "foraeldelse_record",
    indexes = {
      @Index(name = "idx_foraeldelse_record_fordring_id", columnList = "fordring_id"),
      @Index(name = "idx_foraeldelse_record_kompleks_id", columnList = "kompleks_id")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ForaeldelseRecord extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "fordring_id", nullable = false, unique = true)
  private UUID fordringId;

  @Column(name = "debtor_person_id", nullable = false)
  private UUID debtorPersonId;

  @Enumerated(EnumType.STRING)
  @Column(name = "retsgrundlag", nullable = false, length = 30)
  private Retsgrundlag retsgrundlag;

  @Column(name = "udskydelse_dato")
  private LocalDate udskydelseDato;

  @Column(name = "is_in_udskydelse", nullable = false)
  private boolean inUdskydelse;

  @Column(name = "current_frist_expires", nullable = false)
  private LocalDate currentFristExpires;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private ForaeldelseStatus status;

  @Column(name = "kompleks_id")
  private UUID kompleksId;
}
