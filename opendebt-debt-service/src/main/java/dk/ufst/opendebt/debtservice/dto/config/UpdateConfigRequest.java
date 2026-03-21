package dk.ufst.opendebt.debtservice.dto.config;

import java.time.LocalDate;

import jakarta.validation.constraints.*;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateConfigRequest {
  @NotBlank private String configValue;
  private LocalDate validTo;
  private String description;
  private String legalBasis;
  private String reviewStatus; // "APPROVED" to approve a PENDING_REVIEW entry
}
