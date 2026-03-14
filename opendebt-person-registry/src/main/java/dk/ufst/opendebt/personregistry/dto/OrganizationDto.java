package dk.ufst.opendebt.personregistry.dto;

import java.util.UUID;

import dk.ufst.opendebt.personregistry.entity.OrganizationEntity.OrganizationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {

  private UUID organizationId;
  private String cvr;
  private String name;
  private String address;
  private OrganizationType organizationType;
  private Boolean active;
}
