package dk.ufst.opendebt.creditorservice.dto;

import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelBindingDto {

  private UUID id;
  private String channelIdentity;
  private ChannelType channelType;
  private UUID creditorId;
  private Boolean active;
  private String description;
}
