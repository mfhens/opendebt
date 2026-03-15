package dk.ufst.opendebt.creditor.dto;

import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessResolutionRequest {

  private String channelType;
  private String presentedIdentity;
  private UUID representedCreditorOrgId;
}
