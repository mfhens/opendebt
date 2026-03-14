package dk.ufst.opendebt.creditorservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import dk.ufst.opendebt.creditorservice.action.CreditorAction;

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
