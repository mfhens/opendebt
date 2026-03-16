package dk.ufst.opendebt.creditorservice.service.impl;

import java.util.*;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.creditorservice.action.CreditorAction;
import dk.ufst.opendebt.creditorservice.dto.*;
import dk.ufst.opendebt.creditorservice.entity.ActivityStatus;
import dk.ufst.opendebt.creditorservice.entity.CreditorEntity;
import dk.ufst.opendebt.creditorservice.exception.CreditorNotFoundException;
import dk.ufst.opendebt.creditorservice.mapper.CreditorMapper;
import dk.ufst.opendebt.creditorservice.repository.CreditorRepository;
import dk.ufst.opendebt.creditorservice.service.CreditorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditorServiceImpl implements CreditorService {

  private final CreditorRepository creditorRepository;
  private final CreditorMapper creditorMapper;

  @Override
  public CreditorDto getByCreditorOrgId(UUID creditorOrgId) {
    CreditorEntity entity =
        creditorRepository
            .findByCreditorOrgId(creditorOrgId)
            .orElseThrow(
                () ->
                    new CreditorNotFoundException(
                        "Creditor not found with organization ID: " + creditorOrgId));
    return creditorMapper.toDto(entity);
  }

  @Override
  public CreditorDto getByExternalCreditorId(String externalCreditorId) {
    CreditorEntity entity =
        creditorRepository
            .findByExternalCreditorId(externalCreditorId)
            .orElseThrow(
                () ->
                    new CreditorNotFoundException(
                        "Creditor not found with external ID: " + externalCreditorId));
    return creditorMapper.toDto(entity);
  }

  @Override
  public List<CreditorDto> getChildrenByParentId(UUID parentCreditorId) {
    List<CreditorEntity> children = creditorRepository.findByParentCreditorId(parentCreditorId);
    return children.stream().map(creditorMapper::toDto).toList();
  }

  @Override
  public ValidateActionResponse validateAction(UUID creditorOrgId, ValidateActionRequest request) {
    CreditorEntity creditor =
        creditorRepository
            .findByCreditorOrgId(creditorOrgId)
            .orElseThrow(
                () ->
                    new CreditorNotFoundException(
                        "Creditor not found with organization ID: " + creditorOrgId));

    CreditorAction action = request.getRequestedAction();

    if (!isCreditorActive(creditor)) {
      return ValidateActionResponse.builder()
          .allowed(false)
          .requestedAction(action)
          .reasonCode("CREDITOR_INACTIVE")
          .message("Creditor is not active: " + creditor.getActivityStatus())
          .build();
    }

    if (!isActionAllowed(creditor, action)) {
      return ValidateActionResponse.builder()
          .allowed(false)
          .requestedAction(action)
          .reasonCode("ACTION_NOT_PERMITTED")
          .message("Creditor does not have permission for action: " + action)
          .build();
    }

    return ValidateActionResponse.builder().allowed(true).requestedAction(action).build();
  }

  @Override
  public Optional<CreditorDto> findCreditorByCreditorOrgId(UUID creditorOrgId) {
    return creditorRepository.findByCreditorOrgId(creditorOrgId).map(creditorMapper::toDto);
  }

  @Override
  public List<CreditorDto> listActive() {
    return creditorRepository
        .findByActivityStatusOrderByExternalCreditorIdAsc(ActivityStatus.ACTIVE)
        .stream()
        .map(creditorMapper::toDto)
        .toList();
  }

  private boolean isCreditorActive(CreditorEntity creditor) {
    return creditor.getActivityStatus() == ActivityStatus.ACTIVE;
  }

  private boolean isActionAllowed(CreditorEntity creditor, CreditorAction action) {
    return switch (action) {
      case CREATE_CLAIM -> creditor.getAllowCreateRecoveryClaims();
      case UPDATE_CLAIM -> creditor.getAllowWriteDown() || creditor.getAllowWriteUpAdjustment();
      case VIEW_CREDITOR -> true;
      case ADMINISTER_CREDITOR -> creditor.getAllowPortalActions();
    };
  }
}
