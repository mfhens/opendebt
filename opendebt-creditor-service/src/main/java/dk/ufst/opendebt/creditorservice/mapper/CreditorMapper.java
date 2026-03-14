package dk.ufst.opendebt.creditorservice.mapper;

import org.mapstruct.Mapper;

import dk.ufst.opendebt.creditorservice.dto.*;
import dk.ufst.opendebt.creditorservice.entity.*;

@Mapper(componentModel = "spring")
public interface CreditorMapper {

  CreditorDto toDto(CreditorEntity entity);

  CreditorEntity toEntity(CreditorDto dto);
}
