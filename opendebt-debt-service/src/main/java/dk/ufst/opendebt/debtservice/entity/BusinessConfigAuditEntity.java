package dk.ufst.opendebt.debtservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;

/** Immutable audit record for every change to a business_config entry. */
@Entity
@Table(
    name = "business_config_audit",
    indexes = {
      @Index(name = "idx_bca_config_entry_id", columnList = "config_entry_id"),
      @Index(name = "idx_bca_config_key", columnList = "config_key"),
      @Index(name = "idx_bca_performed_at", columnList = "performed_at")
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessConfigAuditEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "config_entry_id", nullable = false)
  private UUID configEntryId;

  @Column(name = "config_key", nullable = false, length = 100)
  private String configKey;

  @Column(name = "action", nullable = false, length = 20)
  private String action; // CREATE, UPDATE, APPROVE, REJECT, DELETE

  @Column(name = "old_value", length = 500)
  private String oldValue;

  @Column(name = "new_value", length = 500)
  private String newValue;

  @Column(name = "performed_by", nullable = false, length = 100)
  private String performedBy;

  @Column(name = "performed_at", nullable = false)
  private LocalDateTime performedAt;

  @Column(name = "details", columnDefinition = "TEXT")
  private String details;
}
