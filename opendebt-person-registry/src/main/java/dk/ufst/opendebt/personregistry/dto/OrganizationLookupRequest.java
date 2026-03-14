package dk.ufst.opendebt.personregistry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import dk.ufst.opendebt.personregistry.entity.OrganizationEntity.OrganizationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationLookupRequest {

  @NotBlank(message = "CVR number is required")
  @Pattern(regexp = "^[0-9]{8}$", message = "CVR must be exactly 8 digits")
  private String cvr;

  private String name;

  private String address;

  private OrganizationType organizationType;
}
