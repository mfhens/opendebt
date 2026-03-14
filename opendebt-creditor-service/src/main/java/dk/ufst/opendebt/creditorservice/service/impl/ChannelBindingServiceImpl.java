package dk.ufst.opendebt.creditorservice.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.creditorservice.dto.*;
import dk.ufst.opendebt.creditorservice.entity.ChannelBindingEntity;
import dk.ufst.opendebt.creditorservice.entity.CreditorEntity;
import dk.ufst.opendebt.creditorservice.exception.ChannelBindingAlreadyExistsException;
import dk.ufst.opendebt.creditorservice.exception.ChannelBindingNotFoundException;
import dk.ufst.opendebt.creditorservice.exception.CreditorNotFoundException;
import dk.ufst.opendebt.creditorservice.mapper.ChannelBindingMapper;
import dk.ufst.opendebt.creditorservice.repository.ChannelBindingRepository;
import dk.ufst.opendebt.creditorservice.repository.CreditorRepository;
import dk.ufst.opendebt.creditorservice.service.ChannelBindingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelBindingServiceImpl implements ChannelBindingService {

  private final ChannelBindingRepository channelBindingRepository;
  private final CreditorRepository creditorRepository;
  private final ChannelBindingMapper channelBindingMapper;

  @Override
  public ChannelBindingDto createBinding(CreateChannelBindingRequest request) {
    if (channelBindingRepository.existsByChannelIdentity(request.getChannelIdentity())) {
      throw new ChannelBindingAlreadyExistsException(
          "Channel binding already exists for identity: " + request.getChannelIdentity());
    }

    creditorRepository
        .findById(request.getCreditorId())
        .orElseThrow(
            () -> new CreditorNotFoundException("Creditor not found: " + request.getCreditorId()));

    ChannelBindingEntity entity =
        ChannelBindingEntity.builder()
            .channelIdentity(request.getChannelIdentity())
            .channelType(request.getChannelType())
            .creditorId(request.getCreditorId())
            .description(request.getDescription())
            .active(true)
            .build();

    ChannelBindingEntity saved = channelBindingRepository.save(entity);
    log.info(
        "AUDIT: action=CREATE_BINDING, channelIdentity={}, channelType={}, creditorId={}",
        saved.getChannelIdentity(),
        saved.getChannelType(),
        saved.getCreditorId());
    return channelBindingMapper.toDto(saved);
  }

  @Override
  public ChannelBindingDto getBindingByIdentity(String channelIdentity) {
    ChannelBindingEntity entity =
        channelBindingRepository
            .findByChannelIdentityAndActiveTrue(channelIdentity)
            .orElseThrow(
                () ->
                    new ChannelBindingNotFoundException(
                        "No active binding for identity: " + channelIdentity));
    return channelBindingMapper.toDto(entity);
  }

  @Override
  public List<ChannelBindingDto> getBindingsByCreditorId(UUID creditorId) {
    return channelBindingRepository.findByCreditorIdAndActiveTrue(creditorId).stream()
        .map(channelBindingMapper::toDto)
        .toList();
  }

  @Override
  public void deactivateBinding(UUID bindingId) {
    ChannelBindingEntity entity =
        channelBindingRepository
            .findById(bindingId)
            .orElseThrow(
                () ->
                    new ChannelBindingNotFoundException("Channel binding not found: " + bindingId));
    entity.setActive(false);
    channelBindingRepository.save(entity);
    log.info(
        "AUDIT: action=DEACTIVATE_BINDING, bindingId={}, channelIdentity={}",
        entity.getId(),
        entity.getChannelIdentity());
  }

  @Override
  public AccessResolutionResponse resolveAccess(AccessResolutionRequest request) {
    log.debug(
        "Resolving access for identity={}, channelType={}, representedCreditorOrgId={}",
        request.getPresentedIdentity(),
        request.getChannelType(),
        request.getRepresentedCreditorOrgId());

    // Step 1: Resolve the presented identity to a bound creditor
    ChannelBindingEntity binding =
        channelBindingRepository
            .findByChannelIdentityAndActiveTrue(request.getPresentedIdentity())
            .orElse(null);

    if (binding == null) {
      log.info(
          "AUDIT: action=ACCESS_DENIED, identity={}, reason=UNBOUND_IDENTITY",
          request.getPresentedIdentity());
      return AccessResolutionResponse.builder()
          .channelType(request.getChannelType())
          .allowed(false)
          .reasonCode("UNBOUND_IDENTITY")
          .message("No active binding exists for the presented identity")
          .build();
    }

    // Step 2: Find the acting creditor
    CreditorEntity actingCreditor =
        creditorRepository
            .findById(binding.getCreditorId())
            .orElseThrow(
                () ->
                    new CreditorNotFoundException(
                        "Bound creditor not found: " + binding.getCreditorId()));

    UUID actingCreditorOrgId = actingCreditor.getCreditorOrgId();

    // Step 3: Check if acting-on-behalf-of is requested
    if (request.getRepresentedCreditorOrgId() != null
        && !request.getRepresentedCreditorOrgId().equals(actingCreditorOrgId)) {

      // Verify the acting creditor is a parent of the represented creditor
      CreditorEntity representedCreditor =
          creditorRepository
              .findByCreditorOrgId(request.getRepresentedCreditorOrgId())
              .orElse(null);

      if (representedCreditor == null) {
        log.info(
            "AUDIT: action=ACCESS_DENIED, identity={}, reason=REPRESENTED_CREDITOR_NOT_FOUND",
            request.getPresentedIdentity());
        return AccessResolutionResponse.builder()
            .channelType(request.getChannelType())
            .actingCreditorOrgId(actingCreditorOrgId)
            .allowed(false)
            .reasonCode("REPRESENTED_CREDITOR_NOT_FOUND")
            .message("Represented creditor not found")
            .build();
      }

      // Check hierarchy: the acting creditor must be the parent of the represented creditor
      if (!actingCreditor.getId().equals(representedCreditor.getParentCreditorId())) {
        log.info(
            "AUDIT: action=ACCESS_DENIED, identity={}, reason=HIERARCHY_NOT_ALLOWED,"
                + " actingCreditorId={}, representedCreditorId={}",
            request.getPresentedIdentity(),
            actingCreditor.getId(),
            representedCreditor.getId());
        return AccessResolutionResponse.builder()
            .channelType(request.getChannelType())
            .actingCreditorOrgId(actingCreditorOrgId)
            .representedCreditorOrgId(request.getRepresentedCreditorOrgId())
            .allowed(false)
            .reasonCode("HIERARCHY_NOT_ALLOWED")
            .message("Acting creditor is not a parent of the represented creditor")
            .build();
      }

      log.info(
          "AUDIT: action=ACCESS_GRANTED, identity={}, actingCreditorOrgId={},"
              + " representedCreditorOrgId={}",
          request.getPresentedIdentity(),
          actingCreditorOrgId,
          request.getRepresentedCreditorOrgId());
      return AccessResolutionResponse.builder()
          .channelType(request.getChannelType())
          .actingCreditorOrgId(actingCreditorOrgId)
          .representedCreditorOrgId(request.getRepresentedCreditorOrgId())
          .allowed(true)
          .build();
    }

    // Direct access — no acting-on-behalf-of
    log.info(
        "AUDIT: action=ACCESS_GRANTED, identity={}, actingCreditorOrgId={}",
        request.getPresentedIdentity(),
        actingCreditorOrgId);
    return AccessResolutionResponse.builder()
        .channelType(request.getChannelType())
        .actingCreditorOrgId(actingCreditorOrgId)
        .allowed(true)
        .build();
  }
}
