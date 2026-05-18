package dk.ufst.opendebt.debtservice.limitation.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = false)
public class RegisterTillaegsfristRequest {

  private String type;
  private LocalDate appliedDate;
  private String legalReference;
}
