package dk.ufst.opendebt.debtservice.attachment;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAttachmentWorkflowRequest {
  private List<UUID> coveredFordringIds;
}
