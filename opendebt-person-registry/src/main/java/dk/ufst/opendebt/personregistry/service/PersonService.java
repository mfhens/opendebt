package dk.ufst.opendebt.personregistry.service;

import java.util.UUID;

import dk.ufst.opendebt.personregistry.dto.GdprExportResponse;
import dk.ufst.opendebt.personregistry.dto.PersonDto;
import dk.ufst.opendebt.personregistry.dto.PersonLookupRequest;
import dk.ufst.opendebt.personregistry.dto.PersonLookupResponse;

public interface PersonService {

  /**
   * Looks up or creates a person by identifier. Returns the technical UUID that other services
   * should use.
   */
  PersonLookupResponse lookupOrCreate(PersonLookupRequest request);

  /** Gets person details by technical ID. Only authorized services should call this. */
  PersonDto getPersonById(UUID personId);

  /** Updates person details. */
  PersonDto updatePerson(UUID personId, PersonDto personDto);

  /** Exports all data for a person (GDPR right to access). */
  GdprExportResponse exportPersonData(UUID personId);

  /** Requests deletion of person data (GDPR right to erasure). */
  void requestDeletion(UUID personId, String reason);

  /** Processes pending deletion requests. */
  int processPendingDeletions();

  /** Checks if person exists by technical ID. */
  boolean exists(UUID personId);
}
