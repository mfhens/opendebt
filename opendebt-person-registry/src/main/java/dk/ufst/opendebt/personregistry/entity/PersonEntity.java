package dk.ufst.opendebt.personregistry.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;

import lombok.*;

/**
 * Central entity for all personal data (PII). This is the ONLY place where GDPR-sensitive data is
 * stored. All other services reference persons by the technical ID (UUID) only.
 */
@Entity
@Table(
    name = "persons",
    indexes = {
      @Index(name = "idx_person_identifier_hash", columnList = "identifier_hash"),
      @Index(
          name = "idx_person_identifier_role",
          columnList = "identifier_hash,role",
          unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // Identification - encrypted and hashed for lookup
  @Column(name = "identifier_encrypted", nullable = false)
  private byte[] identifierEncrypted;

  @Enumerated(EnumType.STRING)
  @Column(name = "identifier_type", nullable = false, length = 3)
  private IdentifierType identifierType;

  @Column(name = "identifier_hash", nullable = false, length = 64)
  private String identifierHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 10)
  private PersonRole role;

  // Personal data - all encrypted
  @Column(name = "name_encrypted")
  private byte[] nameEncrypted;

  @Column(name = "address_street_encrypted")
  private byte[] addressStreetEncrypted;

  @Column(name = "address_city_encrypted")
  private byte[] addressCityEncrypted;

  @Column(name = "address_postal_code_encrypted")
  private byte[] addressPostalCodeEncrypted;

  @Column(name = "address_country_encrypted")
  private byte[] addressCountryEncrypted;

  @Column(name = "email_encrypted")
  private byte[] emailEncrypted;

  @Column(name = "phone_encrypted")
  private byte[] phoneEncrypted;

  // Digital Post / e-Boks status (not PII itself)
  @Column(name = "digital_post_enabled")
  private Boolean digitalPostEnabled;

  @Column(name = "eboks_enabled")
  private Boolean eboksEnabled;

  // GDPR tracking
  @Column(name = "consent_given_at")
  private LocalDateTime consentGivenAt;

  @Column(name = "consent_type", length = 50)
  private String consentType;

  @Column(name = "data_retention_until")
  private LocalDate dataRetentionUntil;

  @Column(name = "deletion_requested_at")
  private LocalDateTime deletionRequestedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "deletion_reason", length = 200)
  private String deletionReason;

  // Metadata
  @CurrentTimestamp(event = EventType.INSERT, source = SourceType.VM)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @CurrentTimestamp(source = SourceType.VM)
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "last_accessed_at")
  private LocalDateTime lastAccessedAt;

  @Column(name = "access_count")
  @Builder.Default
  private Long accessCount = 0L;

  @Version private Long version;

  public enum IdentifierType {
    CPR,
    CVR
  }

  public enum PersonRole {
    PERSONAL,
    BUSINESS
  }

  /**
   * Marks this person's data as deleted (soft delete). Encrypted PII fields are cleared but record
   * remains for referential integrity.
   */
  public void markAsDeleted(String reason) {
    this.deletedAt = LocalDateTime.now();
    this.deletionReason = reason;
    // Clear encrypted PII
    this.nameEncrypted = null;
    this.addressStreetEncrypted = null;
    this.addressCityEncrypted = null;
    this.addressPostalCodeEncrypted = null;
    this.addressCountryEncrypted = null;
    this.emailEncrypted = null;
    this.phoneEncrypted = null;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public void recordAccess() {
    this.lastAccessedAt = LocalDateTime.now();
    this.accessCount = (this.accessCount == null ? 0 : this.accessCount) + 1;
  }
}
