package dk.ufst.opendebt.personregistry.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** GDPR data export response - contains all data held about a person. */
@Data
@Builder
public class GdprExportResponse {

  private UUID personId;
  private LocalDateTime exportedAt;
  private String exportedBy;

  // Identification
  private String identifierType;
  private String identifier; // Decrypted for export
  private String role;

  // Personal data
  private String name;
  private String addressStreet;
  private String addressCity;
  private String addressPostalCode;
  private String addressCountry;
  private String email;
  private String phone;

  // Consent and retention info
  private LocalDateTime consentGivenAt;
  private String consentType;
  private String dataRetentionUntil;

  // Access history
  private LocalDateTime lastAccessedAt;
  private Long accessCount;

  // Record metadata
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
