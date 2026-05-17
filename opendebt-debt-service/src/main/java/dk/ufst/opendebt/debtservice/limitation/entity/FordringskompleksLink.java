package dk.ufst.opendebt.debtservice.limitation.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "fordringskompleks_link",
    indexes = @Index(name = "idx_fordringskompleks_link_kompleks_id", columnList = "kompleks_id"))
@IdClass(FordringskompleksLinkId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FordringskompleksLink {

  @Id
  @Column(name = "kompleks_id", nullable = false)
  private UUID kompleksId;

  @Id
  @Column(name = "fordring_id", nullable = false)
  private UUID fordringId;

  @Column(name = "linked_at", nullable = false)
  private LocalDateTime linkedAt;
}
