package dk.ufst.opendebt.personregistry.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.personregistry.dto.GdprExportResponse;
import dk.ufst.opendebt.personregistry.dto.PersonDto;
import dk.ufst.opendebt.personregistry.dto.PersonLookupRequest;
import dk.ufst.opendebt.personregistry.dto.PersonLookupResponse;
import dk.ufst.opendebt.personregistry.entity.PersonEntity;
import dk.ufst.opendebt.personregistry.repository.PersonRepository;
import dk.ufst.opendebt.personregistry.service.EncryptionService;
import dk.ufst.opendebt.personregistry.service.PersonService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

  private final PersonRepository personRepository;
  private final EncryptionService encryptionService;

  @Override
  @Transactional
  public PersonLookupResponse lookupOrCreate(PersonLookupRequest request) {
    // Hash the identifier for lookup (using identifier type as salt)
    String identifierHash =
        encryptionService.hash(request.getIdentifier(), request.getIdentifierType().name());

    // Map request role to entity role
    PersonEntity.PersonRole role = PersonEntity.PersonRole.valueOf(request.getRole().name());

    // Try to find existing person
    var existingPerson = personRepository.findByIdentifierHashAndRole(identifierHash, role);

    if (existingPerson.isPresent()) {
      PersonEntity person = existingPerson.get();
      person.recordAccess();
      personRepository.save(person);

      log.info("Person lookup: found existing personId={}, role={}", person.getId(), role);
      return PersonLookupResponse.builder()
          .personId(person.getId())
          .created(false)
          .role(role.name())
          .build();
    }

    // Create new person
    PersonEntity newPerson =
        PersonEntity.builder()
            .identifierEncrypted(encryptionService.encrypt(request.getIdentifier()))
            .identifierType(PersonEntity.IdentifierType.valueOf(request.getIdentifierType().name()))
            .identifierHash(identifierHash)
            .role(role)
            .nameEncrypted(encryptionService.encrypt(request.getName()))
            .addressStreetEncrypted(encryptionService.encrypt(request.getAddressStreet()))
            .addressCityEncrypted(encryptionService.encrypt(request.getAddressCity()))
            .addressPostalCodeEncrypted(encryptionService.encrypt(request.getAddressPostalCode()))
            .addressCountryEncrypted(encryptionService.encrypt(request.getAddressCountry()))
            .emailEncrypted(encryptionService.encrypt(request.getEmail()))
            .phoneEncrypted(encryptionService.encrypt(request.getPhone()))
            .digitalPostEnabled(false) // Default, can be updated later
            .eboksEnabled(false)
            .accessCount(1L)
            .lastAccessedAt(LocalDateTime.now())
            .dataRetentionUntil(LocalDate.now().plusYears(10)) // Default 10-year retention
            .build();

    PersonEntity savedPerson = personRepository.save(newPerson);

    log.info(
        "Person created: personId={}, identifierType={}, role={}",
        savedPerson.getId(),
        savedPerson.getIdentifierType(),
        role);

    return PersonLookupResponse.builder()
        .personId(savedPerson.getId())
        .created(true)
        .role(role.name())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public PersonDto getPersonById(UUID personId) {
    PersonEntity person =
        personRepository
            .findById(personId)
            .orElseThrow(() -> new PersonNotFoundException("Person not found: " + personId));

    if (person.isDeleted()) {
      throw new PersonNotFoundException("Person has been deleted: " + personId);
    }

    // Record access (non-transactional side effect)
    person.recordAccess();

    return PersonDto.builder()
        .id(person.getId())
        .identifierType(person.getIdentifierType().name())
        .role(person.getRole().name())
        .name(encryptionService.decrypt(person.getNameEncrypted()))
        .addressStreet(encryptionService.decrypt(person.getAddressStreetEncrypted()))
        .addressCity(encryptionService.decrypt(person.getAddressCityEncrypted()))
        .addressPostalCode(encryptionService.decrypt(person.getAddressPostalCodeEncrypted()))
        .addressCountry(encryptionService.decrypt(person.getAddressCountryEncrypted()))
        .email(encryptionService.decrypt(person.getEmailEncrypted()))
        .phone(encryptionService.decrypt(person.getPhoneEncrypted()))
        .digitalPostEnabled(person.getDigitalPostEnabled())
        .eboksEnabled(person.getEboksEnabled())
        .createdAt(person.getCreatedAt())
        .updatedAt(person.getUpdatedAt())
        .deleted(person.isDeleted())
        .build();
  }

  @Override
  @Transactional
  public PersonDto updatePerson(UUID personId, PersonDto personDto) {
    PersonEntity person =
        personRepository
            .findById(personId)
            .orElseThrow(() -> new PersonNotFoundException("Person not found: " + personId));

    if (person.isDeleted()) {
      throw new IllegalStateException("Cannot update deleted person: " + personId);
    }

    // Update encrypted fields if provided
    if (personDto.getName() != null) {
      person.setNameEncrypted(encryptionService.encrypt(personDto.getName()));
    }
    if (personDto.getAddressStreet() != null) {
      person.setAddressStreetEncrypted(encryptionService.encrypt(personDto.getAddressStreet()));
    }
    if (personDto.getAddressCity() != null) {
      person.setAddressCityEncrypted(encryptionService.encrypt(personDto.getAddressCity()));
    }
    if (personDto.getAddressPostalCode() != null) {
      person.setAddressPostalCodeEncrypted(
          encryptionService.encrypt(personDto.getAddressPostalCode()));
    }
    if (personDto.getAddressCountry() != null) {
      person.setAddressCountryEncrypted(encryptionService.encrypt(personDto.getAddressCountry()));
    }
    if (personDto.getEmail() != null) {
      person.setEmailEncrypted(encryptionService.encrypt(personDto.getEmail()));
    }
    if (personDto.getPhone() != null) {
      person.setPhoneEncrypted(encryptionService.encrypt(personDto.getPhone()));
    }
    if (personDto.getDigitalPostEnabled() != null) {
      person.setDigitalPostEnabled(personDto.getDigitalPostEnabled());
    }
    if (personDto.getEboksEnabled() != null) {
      person.setEboksEnabled(personDto.getEboksEnabled());
    }

    PersonEntity updatedPerson = personRepository.save(person);

    log.info("Person updated: personId={}", personId);

    return getPersonById(updatedPerson.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public GdprExportResponse exportPersonData(UUID personId) {
    PersonEntity person =
        personRepository
            .findById(personId)
            .orElseThrow(() -> new PersonNotFoundException("Person not found: " + personId));

    // Export includes all data, even if deleted
    return GdprExportResponse.builder()
        .personId(person.getId())
        .identifierType(person.getIdentifierType().name())
        .role(person.getRole().name())
        .identifier(encryptionService.decrypt(person.getIdentifierEncrypted()))
        .name(encryptionService.decrypt(person.getNameEncrypted()))
        .addressStreet(encryptionService.decrypt(person.getAddressStreetEncrypted()))
        .addressCity(encryptionService.decrypt(person.getAddressCityEncrypted()))
        .addressPostalCode(encryptionService.decrypt(person.getAddressPostalCodeEncrypted()))
        .addressCountry(encryptionService.decrypt(person.getAddressCountryEncrypted()))
        .email(encryptionService.decrypt(person.getEmailEncrypted()))
        .phone(encryptionService.decrypt(person.getPhoneEncrypted()))
        .digitalPostEnabled(person.getDigitalPostEnabled())
        .eboksEnabled(person.getEboksEnabled())
        .consentGivenAt(person.getConsentGivenAt())
        .consentType(person.getConsentType())
        .dataRetentionUntil(person.getDataRetentionUntil())
        .createdAt(person.getCreatedAt())
        .updatedAt(person.getUpdatedAt())
        .lastAccessedAt(person.getLastAccessedAt())
        .accessCount(person.getAccessCount())
        .deletionRequestedAt(person.getDeletionRequestedAt())
        .deletedAt(person.getDeletedAt())
        .deletionReason(person.getDeletionReason())
        .build();
  }

  @Override
  @Transactional
  public void requestDeletion(UUID personId, String reason) {
    PersonEntity person =
        personRepository
            .findById(personId)
            .orElseThrow(() -> new PersonNotFoundException("Person not found: " + personId));

    if (person.isDeleted()) {
      log.warn("Person already deleted: personId={}", personId);
      return;
    }

    person.setDeletionRequestedAt(LocalDateTime.now());
    person.setDeletionReason(reason);
    personRepository.save(person);

    log.info("Deletion requested for person: personId={}, reason={}", personId, reason);
  }

  @Override
  @Transactional
  public int processPendingDeletions() {
    List<PersonEntity> pendingDeletions =
        personRepository.findByDeletionRequestedAtIsNotNullAndDeletedAtIsNull();
    int count = 0;

    for (PersonEntity person : pendingDeletions) {
      person.markAsDeleted(person.getDeletionReason());
      personRepository.save(person);
      count++;
      log.info(
          "Person data erased: personId={}, reason={}", person.getId(), person.getDeletionReason());
    }

    log.info("Processed {} pending deletions", count);
    return count;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(UUID personId) {
    return personRepository.existsById(personId);
  }

  static final class PersonNotFoundException extends RuntimeException {
    PersonNotFoundException(String message) {
      super(message);
    }
  }
}
