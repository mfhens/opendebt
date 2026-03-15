package dk.ufst.opendebt.creditor.dto;

import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalCreditorDto {

  private UUID id;
  private UUID creditorOrgId;
  private String externalCreditorId;
  private String activityStatus;
  private String connectionType;
  private UUID parentCreditorId;
}
