package dk.ufst.opendebt.creditor.dto;

import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessResolutionResponse {

  private String channelType;
  private UUID actingCreditorOrgId;
  private UUID representedCreditorOrgId;
  private boolean allowed;
  private String reasonCode;
  private String message;
}
