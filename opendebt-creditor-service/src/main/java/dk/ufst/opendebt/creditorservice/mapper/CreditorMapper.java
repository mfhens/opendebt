package dk.ufst.opendebt.creditorservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import dk.ufst.opendebt.creditorservice.dto.*;
import dk.ufst.opendebt.creditorservice.entity.*;

@Mapper(componentModel = "spring")
public interface CreditorMapper {

  @Mapping(target = "displayName", expression = "java(deriveDisplayName(entity))")
  CreditorDto toDto(CreditorEntity entity);

  CreditorEntity toEntity(CreditorDto dto);

  default String deriveDisplayName(CreditorEntity entity) {
    if (entity == null) {
      return null;
    }
    if (entity.getExternalCreditorId() != null && !entity.getExternalCreditorId().isBlank()) {
      return entity.getExternalCreditorId();
    }
    if (entity.getSystemReporterId() != null && !entity.getSystemReporterId().isBlank()) {
      return entity.getSystemReporterId();
    }
    return entity.getCreditorOrgId() != null ? entity.getCreditorOrgId().toString() : null;
  }
}
