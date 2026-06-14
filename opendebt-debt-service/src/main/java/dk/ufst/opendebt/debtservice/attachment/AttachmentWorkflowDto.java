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
public class AttachmentWorkflowDto {
  private UUID workflowId;
  private UUID debtorId;
  private List<UUID> coveredFordringIds;
  private AttachmentWorkflowStatus status;
  private String workflowReference;
  private AttachmentWorkflowOutcomeQualifier outcomeQualifier;
  private AttachmentWorkflowDispatchMetadata dispatchMetadata;
  private String interruptionLinkageMetadata;
  private List<AttachmentWorkflowHistoryEntry> history;
}
