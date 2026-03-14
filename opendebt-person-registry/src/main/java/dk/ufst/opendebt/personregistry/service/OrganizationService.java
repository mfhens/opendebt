package dk.ufst.opendebt.personregistry.service;

import java.util.UUID;

import dk.ufst.opendebt.personregistry.dto.OrganizationDto;
import dk.ufst.opendebt.personregistry.dto.OrganizationLookupRequest;
import dk.ufst.opendebt.personregistry.dto.OrganizationLookupResponse;

public interface OrganizationService {

  /**
   * Looks up or creates an organization by CVR. Returns the technical UUID that other services
   * should use.
   */
  OrganizationLookupResponse lookupOrCreate(OrganizationLookupRequest request);

  /** Gets organization details by technical ID. Only authorized services should call this. */
  OrganizationDto getOrganizationById(UUID organizationId);

  /** Checks if organization exists by technical ID. */
  boolean exists(UUID organizationId);
}
