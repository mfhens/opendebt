package dk.ufst.opendebt.creditorservice.mapper;

import org.mapstruct.Mapper;

import dk.ufst.opendebt.creditorservice.dto.ChannelBindingDto;
import dk.ufst.opendebt.creditorservice.entity.ChannelBindingEntity;

@Mapper(componentModel = "spring")
public interface ChannelBindingMapper {

  ChannelBindingDto toDto(ChannelBindingEntity entity);

  ChannelBindingEntity toEntity(ChannelBindingDto dto);
}
