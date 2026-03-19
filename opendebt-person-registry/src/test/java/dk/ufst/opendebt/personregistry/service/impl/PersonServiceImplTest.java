package dk.ufst.opendebt.personregistry.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.personregistry.dto.GdprExportResponse;
import dk.ufst.opendebt.personregistry.dto.PersonDto;
import dk.ufst.opendebt.personregistry.dto.PersonLookupRequest;
import dk.ufst.opendebt.personregistry.dto.PersonLookupResponse;
import dk.ufst.opendebt.personregistry.entity.PersonEntity;
import dk.ufst.opendebt.personregistry.entity.PersonEntity.IdentifierType;
import dk.ufst.opendebt.personregistry.entity.PersonEntity.PersonRole;
import dk.ufst.opendebt.personregistry.repository.PersonRepository;
import dk.ufst.opendebt.personregistry.service.EncryptionService;
import dk.ufst.opendebt.personregistry.service.impl.PersonServiceImpl.PersonNotFoundException;

@ExtendWith(MockitoExtension.class)
class PersonServiceImplTest {

  @Mock private PersonRepository personRepository;

  @Mock private EncryptionService encryptionService;

  @InjectMocks private PersonServiceImpl personService;

  private static final String CPR = "1234567890";
  private static final String CPR_HASH = "hashed_cpr";
  private static final UUID PERSON_ID = UUID.randomUUID();
  private static final byte[] ENCRYPTED_CPR = "encrypted_cpr".getBytes();
  private static final byte[] ENCRYPTED_NAME = "encrypted_name".getBytes();

  private PersonEntity existingPerson;
  private PersonLookupRequest lookupRequest;

  @BeforeEach
  void setUp() {
    existingPerson =
        PersonEntity.builder()
            .id(PERSON_ID)
            .identifierEncrypted(ENCRYPTED_CPR)
            .identifierType(IdentifierType.CPR)
            .identifierHash(CPR_HASH)
            .role(PersonRole.PERSONAL)
            .nameEncrypted(ENCRYPTED_NAME)
            .digitalPostEnabled(false)
            .eboksEnabled(false)
            .accessCount(0L)
            .dataRetentionUntil(LocalDate.now().plusYears(10))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    lookupRequest =
        PersonLookupRequest.builder()
            .identifier(CPR)
            .identifierType(PersonLookupRequest.IdentifierType.CPR)
            .role(PersonLookupRequest.PersonRole.PERSONAL)
            .name("John Doe")
            .addressStreet("Main Street 1")
            .addressCity("Copenhagen")
            .addressPostalCode("1000")
            .addressCountry("Denmark")
            .email("john@example.com")
            .phone("+4512345678")
            .build();
  }

  @Test
  void lookupOrCreate_existingPerson_returnsExistingId() {
    when(encryptionService.hash(CPR, "CPR")).thenReturn(CPR_HASH);
    when(personRepository.findByIdentifierHashAndRole(CPR_HASH, PersonRole.PERSONAL))
        .thenReturn(Optional.of(existingPerson));

    PersonLookupResponse response = personService.lookupOrCreate(lookupRequest);

    assertThat(response.getPersonId()).isEqualTo(PERSON_ID);
    assertThat(response.isCreated()).isFalse();
    assertThat(response.getRole()).isEqualTo("PERSONAL");
    verify(personRepository).save(existingPerson);
    verify(personRepository, never()).save(argThat(p -> p.getId() == null));
  }

  @Test
  void lookupOrCreate_newPerson_createsAndReturnsId() {
    when(encryptionService.hash(CPR, "CPR")).thenReturn(CPR_HASH);
    when(personRepository.findByIdentifierHashAndRole(CPR_HASH, PersonRole.PERSONAL))
        .thenReturn(Optional.empty());
    when(encryptionService.encrypt(CPR)).thenReturn(ENCRYPTED_CPR);
    when(encryptionService.encrypt("John Doe")).thenReturn(ENCRYPTED_NAME);
    when(encryptionService.encrypt("Main Street 1")).thenReturn("encrypted_street".getBytes());
    when(encryptionService.encrypt("Copenhagen")).thenReturn("encrypted_city".getBytes());
    when(encryptionService.encrypt("1000")).thenReturn("encrypted_postal".getBytes());
    when(encryptionService.encrypt("Denmark")).thenReturn("encrypted_country".getBytes());
    when(encryptionService.encrypt("john@example.com")).thenReturn("encrypted_email".getBytes());
    when(encryptionService.encrypt("+4512345678")).thenReturn("encrypted_phone".getBytes());
    when(personRepository.save(any(PersonEntity.class)))
        .thenAnswer(
            invocation -> {
              PersonEntity entity = invocation.getArgument(0);
              entity.setId(PERSON_ID);
              return entity;
            });

    PersonLookupResponse response = personService.lookupOrCreate(lookupRequest);

    assertThat(response.getPersonId()).isEqualTo(PERSON_ID);
    assertThat(response.isCreated()).isTrue();
    assertThat(response.getRole()).isEqualTo("PERSONAL");

    ArgumentCaptor<PersonEntity> captor = ArgumentCaptor.forClass(PersonEntity.class);
    verify(personRepository).save(captor.capture());
    PersonEntity saved = captor.getValue();
    assertThat(saved.getIdentifierHash()).isEqualTo(CPR_HASH);
    assertThat(saved.getRole()).isEqualTo(PersonRole.PERSONAL);
    assertThat(saved.getIdentifierType()).isEqualTo(IdentifierType.CPR);
    assertThat(saved.getAccessCount()).isEqualTo(1L);
  }

  @Test
  void getPersonById_existingPerson_returnsDto() {
    when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(existingPerson));
    when(encryptionService.decrypt(ENCRYPTED_NAME)).thenReturn("John Doe");
    when(encryptionService.decrypt(null)).thenReturn(null);

    PersonDto dto = personService.getPersonById(PERSON_ID);

    assertThat(dto.getId()).isEqualTo(PERSON_ID);
    assertThat(dto.getName()).isEqualTo("John Doe");
    assertThat(dto.getIdentifierType()).isEqualTo("CPR");
    assertThat(dto.getRole()).isEqualTo("PERSONAL");
    assertThat(dto.isDeleted()).isFalse();
  }

  @Test
  void getPersonById_notFound_throwsException() {
    when(personRepository.findById(PERSON_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> personService.getPersonById(PERSON_ID))
        .isInstanceOf(PersonNotFoundException.class)
        .hasMessageContaining(PERSON_ID.toString());
  }

  @Test
  void getPersonById_deleted_throwsException() {
    PersonEntity deletedPerson = existingPerson;
    deletedPerson.markAsDeleted("Test deletion");

    when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(deletedPerson));

    assertThatThrownBy(() -> personService.getPersonById(PERSON_ID))
        .isInstanceOf(PersonNotFoundException.class)
        .hasMessageContaining("deleted");
  }

  @Test
  void updatePerson_existingPerson_updatesAndReturnsDto() {
    PersonDto updateDto =
        PersonDto.builder()
            .name("Jane Doe")
            .email("jane@example.com")
            .digitalPostEnabled(true)
            .build();

    when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(existingPerson));
    when(encryptionService.encrypt("Jane Doe")).thenReturn("encrypted_jane".getBytes());
    when(encryptionService.encrypt("jane@example.com"))
        .thenReturn("encrypted_jane_email".getBytes());
    when(personRepository.save(any(PersonEntity.class))).thenReturn(existingPerson);
    when(encryptionService.decrypt(any())).thenReturn("decrypted_value");
    when(encryptionService.decrypt(null)).thenReturn(null);

    PersonDto result = personService.updatePerson(PERSON_ID, updateDto);

    assertThat(result.getId()).isEqualTo(PERSON_ID);
    verify(personRepository).save(existingPerson);
    verify(encryptionService).encrypt("Jane Doe");
    verify(encryptionService).encrypt("jane@example.com");
  }

  @Test
  void updatePerson_deletedPerson_throwsException() {
    PersonEntity deletedPerson = existingPerson;
    deletedPerson.markAsDeleted("Test deletion");

    PersonDto updateDto = PersonDto.builder().name("Jane Doe").build();

    when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(deletedPerson));

    assertThatThrownBy(() -> personService.updatePerson(PERSON_ID, updateDto))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot update deleted person");
  }

  @Test
  void exportPersonData_existingPerson_returnsFullData() {
    when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(existingPerson));
    when(encryptionService.decrypt(ENCRYPTED_CPR)).thenReturn(CPR);
    when(encryptionService.decrypt(ENCRYPTED_NAME)).thenReturn("John Doe");
    when(encryptionService.decrypt(null)).thenReturn(null);

    GdprExportResponse response = personService.exportPersonData(PERSON_ID);

    assertThat(response.getPersonId()).isEqualTo(PERSON_ID);
    assertThat(response.getIdentifier()).isEqualTo(CPR);
    assertThat(response.getName()).isEqualTo("John Doe");
    assertThat(response.getIdentifierType()).isEqualTo("CPR");
    assertThat(response.getRole()).isEqualTo("PERSONAL");
  }

  @Test
  void requestDeletion_existingPerson_marksDeletionRequested() {
    when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(existingPerson));
    when(personRepository.save(any(PersonEntity.class))).thenReturn(existingPerson);

    personService.requestDeletion(PERSON_ID, "GDPR erasure request");

    ArgumentCaptor<PersonEntity> captor = ArgumentCaptor.forClass(PersonEntity.class);
    verify(personRepository).save(captor.capture());
    PersonEntity saved = captor.getValue();
    assertThat(saved.getDeletionRequestedAt()).isNotNull();
    assertThat(saved.getDeletionReason()).isEqualTo("GDPR erasure request");
  }

  @Test
  void requestDeletion_alreadyDeleted_doesNothing() {
    PersonEntity deletedPerson = existingPerson;
    deletedPerson.markAsDeleted("Already deleted");

    when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(deletedPerson));

    personService.requestDeletion(PERSON_ID, "Another request");

    // Should log warning and return without saving
    verify(personRepository, never()).save(any());
  }

  @Test
  void processPendingDeletions_processesAllPending() {
    PersonEntity person1 = existingPerson;
    person1.setDeletionRequestedAt(LocalDateTime.now());
    person1.setDeletionReason("Reason 1");

    PersonEntity person2 =
        PersonEntity.builder()
            .id(UUID.randomUUID())
            .identifierEncrypted(ENCRYPTED_CPR)
            .identifierType(IdentifierType.CPR)
            .identifierHash("hash2")
            .role(PersonRole.PERSONAL)
            .deletionRequestedAt(LocalDateTime.now())
            .deletionReason("Reason 2")
            .build();

    when(personRepository.findByDeletionRequestedAtIsNotNullAndDeletedAtIsNull())
        .thenReturn(List.of(person1, person2));
    when(personRepository.save(any(PersonEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    int count = personService.processPendingDeletions();

    assertThat(count).isEqualTo(2);
    verify(personRepository, times(2)).save(any(PersonEntity.class));
  }

  @Test
  void exists_existingPerson_returnsTrue() {
    when(personRepository.existsById(PERSON_ID)).thenReturn(true);

    assertThat(personService.exists(PERSON_ID)).isTrue();
  }

  @Test
  void exists_nonExistingPerson_returnsFalse() {
    when(personRepository.existsById(PERSON_ID)).thenReturn(false);

    assertThat(personService.exists(PERSON_ID)).isFalse();
  }
}
