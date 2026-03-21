package dk.ufst.opendebt.caseworker.dto.config;

import java.time.LocalDate;

import lombok.*;

/** Portal-facing request DTO for creating a new versioned business config entry. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConfigPortalRequest {
  private String configKey;
  private String configValue;
  private String valueType; // DECIMAL, INTEGER, STRING, BOOLEAN
  private LocalDate validFrom;
  private LocalDate validTo;
  private String description;
  private String legalBasis;
}
