package dk.ufst.opendebt.gateway.creditor.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessResolutionRequest {

  @NotNull private ChannelType channelType;
  @NotNull private String presentedIdentity;
  private UUID representedCreditorOrgId;
  private CreditorAction requestedAction;
}
