package dk.ufst.opendebt.personregistry.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** Full person details DTO - only returned to authorized services. */
@Data
@Builder
public class PersonDto {

  private UUID id;

  private String identifierType;
  private String role;

  // PII fields - decrypted
  private String name;
  private String addressStreet;
  private String addressCity;
  private String addressPostalCode;
  private String addressCountry;
  private String email;
  private String phone;

  // Digital communication preferences
  private Boolean digitalPostEnabled;
  private Boolean eboksEnabled;

  // Metadata
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private boolean deleted;
}
