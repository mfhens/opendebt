package dk.ufst.opendebt.creditorservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChannelBindingRequest {

  @NotNull private ChannelType channelType;

  @NotBlank private String channelIdentity;

  @NotNull private UUID creditorId;

  private String description;
}
