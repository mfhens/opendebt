package dk.ufst.opendebt.personregistry.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

/**
 * Central entity for organization/creditor data. Stores information about fordringshavere (creditor
 * institutions).
 */
@Entity
@Table(
    name = "organizations",
    indexes = {@Index(name = "idx_org_cvr_hash", columnList = "cvr_hash", unique = true)})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "cvr_encrypted", nullable = false)
  private byte[] cvrEncrypted;

  @Column(name = "cvr_hash", nullable = false, length = 64, unique = true)
  private String cvrHash;

  @Column(name = "name_encrypted")
  private byte[] nameEncrypted;

  @Column(name = "address_encrypted")
  private byte[] addressEncrypted;

  @Column(name = "contact_email_encrypted")
  private byte[] contactEmailEncrypted;

  @Column(name = "contact_phone_encrypted")
  private byte[] contactPhoneEncrypted;

  // Organization type
  @Enumerated(EnumType.STRING)
  @Column(name = "organization_type", length = 30)
  private OrganizationType organizationType;

  // Status
  @Column(name = "active")
  @Builder.Default
  private Boolean active = true;

  @Column(name = "onboarded_at")
  private LocalDateTime onboardedAt;

  // Metadata
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Version private Long version;

  public enum OrganizationType {
    MUNICIPALITY, // Kommune
    REGION, // Region
    STATE_AGENCY, // Statslig myndighed
    PUBLIC_INSTITUTION, // Offentlig institution
    OTHER
  }
}
