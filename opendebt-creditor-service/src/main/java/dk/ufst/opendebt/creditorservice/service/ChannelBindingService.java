package dk.ufst.opendebt.creditorservice.service;

import java.util.List;
import java.util.UUID;

import dk.ufst.opendebt.creditorservice.dto.*;

public interface ChannelBindingService {

  ChannelBindingDto createBinding(CreateChannelBindingRequest request);

  ChannelBindingDto getBindingByIdentity(String channelIdentity);

  List<ChannelBindingDto> getBindingsByCreditorId(UUID creditorId);

  void deactivateBinding(UUID bindingId);

  AccessResolutionResponse resolveAccess(AccessResolutionRequest request);
}
