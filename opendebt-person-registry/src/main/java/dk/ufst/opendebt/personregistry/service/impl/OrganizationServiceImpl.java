package dk.ufst.opendebt.personregistry.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.personregistry.dto.OrganizationDto;
import dk.ufst.opendebt.personregistry.dto.OrganizationLookupRequest;
import dk.ufst.opendebt.personregistry.dto.OrganizationLookupResponse;
import dk.ufst.opendebt.personregistry.entity.OrganizationEntity;
import dk.ufst.opendebt.personregistry.repository.OrganizationRepository;
import dk.ufst.opendebt.personregistry.service.EncryptionService;
import dk.ufst.opendebt.personregistry.service.OrganizationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

  private static final String CVR_HASH_SALT = "CVR";

  private final OrganizationRepository organizationRepository;
  private final EncryptionService encryptionService;

  @Override
  @Transactional
  public OrganizationLookupResponse lookupOrCreate(OrganizationLookupRequest request) {
    String cvrHash = encryptionService.hash(request.getCvr(), CVR_HASH_SALT);

    OrganizationEntity entity =
        organizationRepository
            .findByCvrHash(cvrHash)
            .orElseGet(() -> createOrganization(request, cvrHash));

    log.debug("Organization lookup: cvr_hash={}, organization_id={}", cvrHash, entity.getId());
    return OrganizationLookupResponse.builder().organizationId(entity.getId()).build();
  }

  @Override
  @Transactional(readOnly = true)
  public OrganizationDto getOrganizationById(UUID organizationId) {
    OrganizationEntity entity =
        organizationRepository
            .findById(organizationId)
            .orElseThrow(
                () ->
                    new OrganizationNotFoundException("Organization not found: " + organizationId));

    return toDto(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(UUID organizationId) {
    return organizationRepository.existsById(organizationId);
  }

  private OrganizationEntity createOrganization(OrganizationLookupRequest request, String cvrHash) {
    OrganizationEntity entity =
        OrganizationEntity.builder()
            .cvrEncrypted(encryptionService.encrypt(request.getCvr()))
            .cvrHash(cvrHash)
            .nameEncrypted(encryptionService.encrypt(request.getName()))
            .addressEncrypted(encryptionService.encrypt(request.getAddress()))
            .organizationType(request.getOrganizationType())
            .active(true)
            .onboardedAt(LocalDateTime.now())
            .build();

    entity = organizationRepository.save(entity);
    log.info("Created new organization: organization_id={}", entity.getId());
    return entity;
  }

  private OrganizationDto toDto(OrganizationEntity entity) {
    return OrganizationDto.builder()
        .organizationId(entity.getId())
        .cvr(encryptionService.decrypt(entity.getCvrEncrypted()))
        .name(encryptionService.decrypt(entity.getNameEncrypted()))
        .address(encryptionService.decrypt(entity.getAddressEncrypted()))
        .organizationType(entity.getOrganizationType())
        .active(entity.getActive())
        .build();
  }

  public static class OrganizationNotFoundException extends RuntimeException {
    public OrganizationNotFoundException(String message) {
      super(message);
    }
  }
}
