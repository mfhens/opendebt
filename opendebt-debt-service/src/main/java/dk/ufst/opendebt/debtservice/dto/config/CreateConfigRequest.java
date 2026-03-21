package dk.ufst.opendebt.debtservice.dto.config;

import java.time.LocalDate;

import jakarta.validation.constraints.*;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConfigRequest {
  @NotBlank private String configKey;
  @NotBlank private String configValue;
  @NotBlank private String valueType; // DECIMAL, INTEGER, STRING, BOOLEAN
  @NotNull private LocalDate validFrom;
  private LocalDate validTo;
  @NotBlank private String description;
  @NotBlank private String legalBasis;
  private boolean seedMigration = false; // if true + caller is ADMIN, allows past validFrom
}
