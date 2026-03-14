package dk.ufst.opendebt.personregistry.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.personregistry.dto.OrganizationDto;
import dk.ufst.opendebt.personregistry.dto.OrganizationLookupRequest;
import dk.ufst.opendebt.personregistry.dto.OrganizationLookupResponse;
import dk.ufst.opendebt.personregistry.entity.OrganizationEntity;
import dk.ufst.opendebt.personregistry.entity.OrganizationEntity.OrganizationType;
import dk.ufst.opendebt.personregistry.repository.OrganizationRepository;
import dk.ufst.opendebt.personregistry.service.EncryptionService;
import dk.ufst.opendebt.personregistry.service.impl.OrganizationServiceImpl.OrganizationNotFoundException;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {

  @Mock private OrganizationRepository organizationRepository;

  @Mock private EncryptionService encryptionService;

  @InjectMocks private OrganizationServiceImpl organizationService;

  private static final String CVR = "12345678";
  private static final String CVR_HASH = "hashed_cvr";
  private static final UUID ORG_ID = UUID.randomUUID();
  private static final byte[] ENCRYPTED_CVR = "encrypted".getBytes();

  private OrganizationEntity existingOrganization;
  private OrganizationLookupRequest lookupRequest;

  @BeforeEach
  void setUp() {
    existingOrganization =
        OrganizationEntity.builder()
            .id(ORG_ID)
            .cvrEncrypted(ENCRYPTED_CVR)
            .cvrHash(CVR_HASH)
            .nameEncrypted("encrypted_name".getBytes())
            .organizationType(OrganizationType.MUNICIPALITY)
            .active(true)
            .build();

    lookupRequest =
        OrganizationLookupRequest.builder()
            .cvr(CVR)
            .name("Test Municipality")
            .organizationType(OrganizationType.MUNICIPALITY)
            .build();
  }

  @Test
  void lookupOrCreate_existingOrganization_returnsExistingId() {
    when(encryptionService.hash(CVR, "CVR")).thenReturn(CVR_HASH);
    when(organizationRepository.findByCvrHash(CVR_HASH))
        .thenReturn(Optional.of(existingOrganization));

    OrganizationLookupResponse response = organizationService.lookupOrCreate(lookupRequest);

    assertThat(response.getOrganizationId()).isEqualTo(ORG_ID);
    verify(organizationRepository, never()).save(any());
  }

  @Test
  void lookupOrCreate_newOrganization_createsAndReturnsId() {
    when(encryptionService.hash(CVR, "CVR")).thenReturn(CVR_HASH);
    when(organizationRepository.findByCvrHash(CVR_HASH)).thenReturn(Optional.empty());
    when(encryptionService.encrypt(CVR)).thenReturn(ENCRYPTED_CVR);
    when(encryptionService.encrypt("Test Municipality")).thenReturn("encrypted_name".getBytes());
    when(encryptionService.encrypt(null)).thenReturn(new byte[0]);
    when(organizationRepository.save(any(OrganizationEntity.class)))
        .thenAnswer(
            invocation -> {
              OrganizationEntity entity = invocation.getArgument(0);
              entity.setId(ORG_ID);
              return entity;
            });

    OrganizationLookupResponse response = organizationService.lookupOrCreate(lookupRequest);

    assertThat(response.getOrganizationId()).isEqualTo(ORG_ID);

    ArgumentCaptor<OrganizationEntity> captor = ArgumentCaptor.forClass(OrganizationEntity.class);
    verify(organizationRepository).save(captor.capture());
    OrganizationEntity saved = captor.getValue();
    assertThat(saved.getCvrHash()).isEqualTo(CVR_HASH);
    assertThat(saved.getOrganizationType()).isEqualTo(OrganizationType.MUNICIPALITY);
    assertThat(saved.getActive()).isTrue();
  }

  @Test
  void getOrganizationById_existingOrganization_returnsDto() {
    when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(existingOrganization));
    when(encryptionService.decrypt(ENCRYPTED_CVR)).thenReturn(CVR);
    when(encryptionService.decrypt("encrypted_name".getBytes())).thenReturn("Test Municipality");
    when(encryptionService.decrypt(null)).thenReturn(null);

    OrganizationDto dto = organizationService.getOrganizationById(ORG_ID);

    assertThat(dto.getOrganizationId()).isEqualTo(ORG_ID);
    assertThat(dto.getCvr()).isEqualTo(CVR);
    assertThat(dto.getName()).isEqualTo("Test Municipality");
    assertThat(dto.getOrganizationType()).isEqualTo(OrganizationType.MUNICIPALITY);
    assertThat(dto.getActive()).isTrue();
  }

  @Test
  void getOrganizationById_notFound_throwsException() {
    when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> organizationService.getOrganizationById(ORG_ID))
        .isInstanceOf(OrganizationNotFoundException.class)
        .hasMessageContaining(ORG_ID.toString());
  }

  @Test
  void exists_existingOrganization_returnsTrue() {
    when(organizationRepository.existsById(ORG_ID)).thenReturn(true);

    assertThat(organizationService.exists(ORG_ID)).isTrue();
  }

  @Test
  void exists_nonExistingOrganization_returnsFalse() {
    when(organizationRepository.existsById(ORG_ID)).thenReturn(false);

    assertThat(organizationService.exists(ORG_ID)).isFalse();
  }
}
