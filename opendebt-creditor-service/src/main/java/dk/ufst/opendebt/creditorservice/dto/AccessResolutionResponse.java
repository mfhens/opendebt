package dk.ufst.opendebt.creditorservice.dto;

import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessResolutionResponse {

  private ChannelType channelType;
  private UUID actingCreditorOrgId;
  private UUID representedCreditorOrgId;
  private boolean allowed;
  private String reasonCode;
  private String message;
}
