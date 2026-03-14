package dk.ufst.opendebt.creditorservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import dk.ufst.opendebt.creditorservice.action.CreditorAction;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateActionRequest {

  @NotNull private CreditorAction requestedAction;
  private UUID representedCreditorOrgId;
  private ChannelType channelType;
}
