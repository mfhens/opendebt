package dk.ufst.opendebt.creditorservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.creditorservice.config.AccessResolutionMetrics;
import dk.ufst.opendebt.creditorservice.dto.*;
import dk.ufst.opendebt.creditorservice.entity.ChannelBindingEntity;
import dk.ufst.opendebt.creditorservice.entity.CreditorEntity;
import dk.ufst.opendebt.creditorservice.exception.ChannelBindingAlreadyExistsException;
import dk.ufst.opendebt.creditorservice.exception.ChannelBindingNotFoundException;
import dk.ufst.opendebt.creditorservice.exception.CreditorNotFoundException;
import dk.ufst.opendebt.creditorservice.mapper.ChannelBindingMapper;
import dk.ufst.opendebt.creditorservice.repository.ChannelBindingRepository;
import dk.ufst.opendebt.creditorservice.repository.CreditorRepository;
import dk.ufst.opendebt.creditorservice.service.impl.ChannelBindingServiceImpl;

@ExtendWith(MockitoExtension.class)
class ChannelBindingServiceImplTest {

  @Mock private ChannelBindingRepository channelBindingRepository;
  @Mock private CreditorRepository creditorRepository;
  @Mock private ChannelBindingMapper channelBindingMapper;
  @Mock private AccessResolutionMetrics accessResolutionMetrics;

  @InjectMocks private ChannelBindingServiceImpl channelBindingService;

  private static final UUID CREDITOR_ID = UUID.randomUUID();
  private static final UUID CREDITOR_ORG_ID = UUID.randomUUID();
  private static final UUID PARENT_CREDITOR_ID = UUID.randomUUID();
  private static final UUID PARENT_CREDITOR_ORG_ID = UUID.randomUUID();
  private static final UUID CHILD_CREDITOR_ORG_ID = UUID.randomUUID();
  private static final String M2M_IDENTITY = "CERT-123";
  private static final String PORTAL_IDENTITY = "USER-456";

  private CreditorEntity parentCreditor;
  private CreditorEntity childCreditor;

  @BeforeEach
  void setUp() {
    parentCreditor =
        CreditorEntity.builder()
            .id(PARENT_CREDITOR_ID)
            .creditorOrgId(PARENT_CREDITOR_ORG_ID)
            .externalCreditorId("K_PARENT")
            .build();

    childCreditor =
        CreditorEntity.builder()
            .id(UUID.randomUUID())
            .creditorOrgId(CHILD_CREDITOR_ORG_ID)
            .externalCreditorId("K_CHILD")
            .parentCreditorId(PARENT_CREDITOR_ID)
            .build();
  }

  @Nested
  @DisplayName("createBinding")
  class CreateBinding {

    @Test
    @DisplayName("creates a new M2M binding successfully")
    void createsM2mBinding() {
      CreateChannelBindingRequest request =
          CreateChannelBindingRequest.builder()
              .channelType(ChannelType.M2M)
              .channelIdentity(M2M_IDENTITY)
              .creditorId(CREDITOR_ID)
              .description("Test M2M certificate")
              .build();

      CreditorEntity creditor =
          CreditorEntity.builder()
              .id(CREDITOR_ID)
              .creditorOrgId(CREDITOR_ORG_ID)
              .externalCreditorId("K1")
              .build();

      ChannelBindingEntity savedEntity =
          ChannelBindingEntity.builder()
              .id(UUID.randomUUID())
              .channelIdentity(M2M_IDENTITY)
              .channelType(ChannelType.M2M)
              .creditorId(CREDITOR_ID)
              .active(true)
              .build();

      ChannelBindingDto expectedDto =
          ChannelBindingDto.builder()
              .channelIdentity(M2M_IDENTITY)
              .channelType(ChannelType.M2M)
              .creditorId(CREDITOR_ID)
              .active(true)
              .build();

      when(channelBindingRepository.existsByChannelIdentity(M2M_IDENTITY)).thenReturn(false);
      when(creditorRepository.findById(CREDITOR_ID)).thenReturn(Optional.of(creditor));
      when(channelBindingRepository.save(any())).thenReturn(savedEntity);
      when(channelBindingMapper.toDto(savedEntity)).thenReturn(expectedDto);

      ChannelBindingDto result = channelBindingService.createBinding(request);

      assertThat(result.getChannelIdentity()).isEqualTo(M2M_IDENTITY);
      assertThat(result.getChannelType()).isEqualTo(ChannelType.M2M);
      assertThat(result.getCreditorId()).isEqualTo(CREDITOR_ID);
      assertThat(result.getActive()).isTrue();
    }

    @Test
    @DisplayName("rejects duplicate channel identity")
    void rejectsDuplicateIdentity() {
      CreateChannelBindingRequest request =
          CreateChannelBindingRequest.builder()
              .channelType(ChannelType.M2M)
              .channelIdentity(M2M_IDENTITY)
              .creditorId(CREDITOR_ID)
              .build();

      when(channelBindingRepository.existsByChannelIdentity(M2M_IDENTITY)).thenReturn(true);

      assertThatThrownBy(() -> channelBindingService.createBinding(request))
          .isInstanceOf(ChannelBindingAlreadyExistsException.class);
    }

    @Test
    @DisplayName("rejects binding to non-existent creditor")
    void rejectsNonExistentCreditor() {
      CreateChannelBindingRequest request =
          CreateChannelBindingRequest.builder()
              .channelType(ChannelType.PORTAL)
              .channelIdentity(PORTAL_IDENTITY)
              .creditorId(CREDITOR_ID)
              .build();

      when(channelBindingRepository.existsByChannelIdentity(PORTAL_IDENTITY)).thenReturn(false);
      when(creditorRepository.findById(CREDITOR_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> channelBindingService.createBinding(request))
          .isInstanceOf(CreditorNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("resolveAccess")
  class ResolveAccess {

    @Test
    @DisplayName("bound M2M identity resolves to a fordringshaver")
    void boundM2mIdentityResolvesToCreditor() {
      ChannelBindingEntity binding =
          ChannelBindingEntity.builder()
              .id(UUID.randomUUID())
              .channelIdentity(M2M_IDENTITY)
              .channelType(ChannelType.M2M)
              .creditorId(CREDITOR_ID)
              .active(true)
              .build();

      CreditorEntity creditor =
          CreditorEntity.builder()
              .id(CREDITOR_ID)
              .creditorOrgId(CREDITOR_ORG_ID)
              .externalCreditorId("K1")
              .build();

      when(channelBindingRepository.findByChannelIdentityAndActiveTrue(M2M_IDENTITY))
          .thenReturn(Optional.of(binding));
      when(creditorRepository.findById(CREDITOR_ID)).thenReturn(Optional.of(creditor));

      AccessResolutionRequest request =
          AccessResolutionRequest.builder()
              .channelType(ChannelType.M2M)
              .presentedIdentity(M2M_IDENTITY)
              .build();

      AccessResolutionResponse response = channelBindingService.resolveAccess(request);

      assertThat(response.isAllowed()).isTrue();
      assertThat(response.getActingCreditorOrgId()).isEqualTo(CREDITOR_ORG_ID);
      assertThat(response.getChannelType()).isEqualTo(ChannelType.M2M);
    }

    @Test
    @DisplayName("unbound identity is rejected")
    void unboundIdentityIsRejected() {
      when(channelBindingRepository.findByChannelIdentityAndActiveTrue("UNKNOWN"))
          .thenReturn(Optional.empty());

      AccessResolutionRequest request =
          AccessResolutionRequest.builder()
              .channelType(ChannelType.M2M)
              .presentedIdentity("UNKNOWN")
              .build();

      AccessResolutionResponse response = channelBindingService.resolveAccess(request);

      assertThat(response.isAllowed()).isFalse();
      assertThat(response.getReasonCode()).isEqualTo("UNBOUND_IDENTITY");
    }

    @Test
    @DisplayName("parent may act on behalf of child fordringshaver")
    void parentMayActOnBehalfOfChild() {
      ChannelBindingEntity binding =
          ChannelBindingEntity.builder()
              .id(UUID.randomUUID())
              .channelIdentity(PORTAL_IDENTITY)
              .channelType(ChannelType.PORTAL)
              .creditorId(PARENT_CREDITOR_ID)
              .active(true)
              .build();

      when(channelBindingRepository.findByChannelIdentityAndActiveTrue(PORTAL_IDENTITY))
          .thenReturn(Optional.of(binding));
      when(creditorRepository.findById(PARENT_CREDITOR_ID)).thenReturn(Optional.of(parentCreditor));
      when(creditorRepository.findByCreditorOrgId(CHILD_CREDITOR_ORG_ID))
          .thenReturn(Optional.of(childCreditor));

      AccessResolutionRequest request =
          AccessResolutionRequest.builder()
              .channelType(ChannelType.PORTAL)
              .presentedIdentity(PORTAL_IDENTITY)
              .representedCreditorOrgId(CHILD_CREDITOR_ORG_ID)
              .build();

      AccessResolutionResponse response = channelBindingService.resolveAccess(request);

      assertThat(response.isAllowed()).isTrue();
      assertThat(response.getActingCreditorOrgId()).isEqualTo(PARENT_CREDITOR_ORG_ID);
      assertThat(response.getRepresentedCreditorOrgId()).isEqualTo(CHILD_CREDITOR_ORG_ID);
    }

    @Test
    @DisplayName("non-parent acting on behalf of another creditor is rejected")
    void nonParentActingOnBehalfIsRejected() {
      UUID unrelatedCreditorId = UUID.randomUUID();
      UUID unrelatedCreditorOrgId = UUID.randomUUID();

      CreditorEntity unrelatedCreditor =
          CreditorEntity.builder()
              .id(unrelatedCreditorId)
              .creditorOrgId(unrelatedCreditorOrgId)
              .externalCreditorId("K_UNRELATED")
              .build();

      ChannelBindingEntity binding =
          ChannelBindingEntity.builder()
              .id(UUID.randomUUID())
              .channelIdentity(PORTAL_IDENTITY)
              .channelType(ChannelType.PORTAL)
              .creditorId(unrelatedCreditorId)
              .active(true)
              .build();

      when(channelBindingRepository.findByChannelIdentityAndActiveTrue(PORTAL_IDENTITY))
          .thenReturn(Optional.of(binding));
      when(creditorRepository.findById(unrelatedCreditorId))
          .thenReturn(Optional.of(unrelatedCreditor));
      when(creditorRepository.findByCreditorOrgId(CHILD_CREDITOR_ORG_ID))
          .thenReturn(Optional.of(childCreditor));

      AccessResolutionRequest request =
          AccessResolutionRequest.builder()
              .channelType(ChannelType.PORTAL)
              .presentedIdentity(PORTAL_IDENTITY)
              .representedCreditorOrgId(CHILD_CREDITOR_ORG_ID)
              .build();

      AccessResolutionResponse response = channelBindingService.resolveAccess(request);

      assertThat(response.isAllowed()).isFalse();
      assertThat(response.getReasonCode()).isEqualTo("HIERARCHY_NOT_ALLOWED");
    }

    @Test
    @DisplayName("acting on behalf of non-existent represented creditor is rejected")
    void nonExistentRepresentedCreditorIsRejected() {
      UUID unknownOrgId = UUID.randomUUID();

      ChannelBindingEntity binding =
          ChannelBindingEntity.builder()
              .id(UUID.randomUUID())
              .channelIdentity(PORTAL_IDENTITY)
              .channelType(ChannelType.PORTAL)
              .creditorId(PARENT_CREDITOR_ID)
              .active(true)
              .build();

      when(channelBindingRepository.findByChannelIdentityAndActiveTrue(PORTAL_IDENTITY))
          .thenReturn(Optional.of(binding));
      when(creditorRepository.findById(PARENT_CREDITOR_ID)).thenReturn(Optional.of(parentCreditor));
      when(creditorRepository.findByCreditorOrgId(unknownOrgId)).thenReturn(Optional.empty());

      AccessResolutionRequest request =
          AccessResolutionRequest.builder()
              .channelType(ChannelType.PORTAL)
              .presentedIdentity(PORTAL_IDENTITY)
              .representedCreditorOrgId(unknownOrgId)
              .build();

      AccessResolutionResponse response = channelBindingService.resolveAccess(request);

      assertThat(response.isAllowed()).isFalse();
      assertThat(response.getReasonCode()).isEqualTo("REPRESENTED_CREDITOR_NOT_FOUND");
    }
  }

  @Nested
  @DisplayName("deactivateBinding")
  class DeactivateBinding {

    @Test
    @DisplayName("deactivates an existing binding")
    void deactivatesBinding() {
      UUID bindingId = UUID.randomUUID();
      ChannelBindingEntity entity =
          ChannelBindingEntity.builder()
              .id(bindingId)
              .channelIdentity(M2M_IDENTITY)
              .channelType(ChannelType.M2M)
              .creditorId(CREDITOR_ID)
              .active(true)
              .build();

      when(channelBindingRepository.findById(bindingId)).thenReturn(Optional.of(entity));
      when(channelBindingRepository.save(any())).thenReturn(entity);

      channelBindingService.deactivateBinding(bindingId);

      assertThat(entity.getActive()).isFalse();
      verify(channelBindingRepository).save(entity);
    }

    @Test
    @DisplayName("throws exception when binding not found")
    void throwsWhenBindingNotFound() {
      UUID bindingId = UUID.randomUUID();
      when(channelBindingRepository.findById(bindingId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> channelBindingService.deactivateBinding(bindingId))
          .isInstanceOf(ChannelBindingNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("getBindingsByCreditorId")
  class GetBindingsByCreditorId {

    @Test
    @DisplayName("returns active bindings for a creditor")
    void returnsActiveBindings() {
      ChannelBindingEntity entity =
          ChannelBindingEntity.builder()
              .id(UUID.randomUUID())
              .channelIdentity(M2M_IDENTITY)
              .channelType(ChannelType.M2M)
              .creditorId(CREDITOR_ID)
              .active(true)
              .build();

      ChannelBindingDto dto =
          ChannelBindingDto.builder()
              .channelIdentity(M2M_IDENTITY)
              .channelType(ChannelType.M2M)
              .creditorId(CREDITOR_ID)
              .active(true)
              .build();

      when(channelBindingRepository.findByCreditorIdAndActiveTrue(CREDITOR_ID))
          .thenReturn(List.of(entity));
      when(channelBindingMapper.toDto(entity)).thenReturn(dto);

      List<ChannelBindingDto> result = channelBindingService.getBindingsByCreditorId(CREDITOR_ID);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getChannelIdentity()).isEqualTo(M2M_IDENTITY);
    }
  }
}
