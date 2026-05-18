package dk.ufst.opendebt.caseservice.limitation.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLimitationObjectionWorkflowRequest {

  private UUID fordringId;
  private UUID debtorPersonId;
  private String registeredBy;
  private String sourceSurface;
  private String sourceReference;
}
