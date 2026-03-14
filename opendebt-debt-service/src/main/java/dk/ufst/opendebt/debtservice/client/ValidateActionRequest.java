package dk.ufst.opendebt.debtservice.client;

import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateActionRequest {

  private CreditorAction requestedAction;
  private UUID representedCreditorOrgId;
  private ChannelType channelType;
}
