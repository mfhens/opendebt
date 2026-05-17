package dk.ufst.opendebt.debtservice.limitation.client.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateObjectionWorkflowRequest {

  private UUID fordringId;
  private UUID debtorPersonId;
  private String registeredBy;
  private String sourceSurface;
  private String sourceReference;
}
