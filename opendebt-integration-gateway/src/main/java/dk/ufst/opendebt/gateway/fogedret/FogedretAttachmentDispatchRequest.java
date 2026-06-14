package dk.ufst.opendebt.gateway.fogedret;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FogedretAttachmentDispatchRequest {
  private UUID debtorId;
  private UUID workflowId;
}
