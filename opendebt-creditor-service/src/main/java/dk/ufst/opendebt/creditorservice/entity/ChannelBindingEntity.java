package dk.ufst.opendebt.creditorservice.entity;

import java.util.UUID;

import jakarta.persistence.*;

import dk.ufst.opendebt.common.audit.AuditableEntity;
import dk.ufst.opendebt.creditorservice.dto.ChannelType;

import lombok.*;

/**
 * Maps an external channel identity (M2M certificate or portal user) to a fordringshaver. Supports
 * binding audit and history via database triggers (V2 migration).
 */
@Entity
@Table(
    name = "channel_bindings",
    indexes = {
      @Index(name = "idx_cb_channel_identity", columnList = "channel_identity", unique = true),
      @Index(name = "idx_cb_creditor_id", columnList = "creditor_id"),
      @Index(name = "idx_cb_channel_type", columnList = "channel_type"),
      @Index(name = "idx_cb_active", columnList = "active")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelBindingEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /**
   * The external identity string presented by the channel (e.g. M2M certificate subject, portal
   * user ID).
   */
  @Column(name = "channel_identity", nullable = false, length = 255, unique = true)
  private String channelIdentity;

  /** The channel type: M2M or PORTAL. */
  @Enumerated(EnumType.STRING)
  @Column(name = "channel_type", nullable = false, length = 20)
  private ChannelType channelType;

  /** FK to the creditor this identity is bound to. */
  @Column(name = "creditor_id", nullable = false)
  private UUID creditorId;

  /** Whether this binding is currently active. */
  @Column(name = "active", nullable = false)
  @Builder.Default
  private Boolean active = true;

  /** Optional human-readable description of the binding. */
  @Column(name = "description", length = 500)
  private String description;

  // Audit fields inherited from AuditableEntity
}
