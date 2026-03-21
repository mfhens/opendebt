package dk.ufst.opendebt.caseworker.dto.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;

/** Portal-facing DTO for a single versioned business configuration entry. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigEntryPortalDto {
  private UUID id;
  private String configKey;
  private String configValue;
  private String valueType;
  private LocalDate validFrom;
  private LocalDate validTo;
  private String description;
  private String legalBasis;
  private String createdBy;
  private LocalDateTime createdAt;
  private String reviewStatus; // null, "PENDING_REVIEW", or "APPROVED"
  private String computedStatus; // "ACTIVE", "FUTURE", "EXPIRED", "PENDING_REVIEW"
}
