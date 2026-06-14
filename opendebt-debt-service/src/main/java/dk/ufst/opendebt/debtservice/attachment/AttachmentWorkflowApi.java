package dk.ufst.opendebt.debtservice.attachment;

import java.util.List;
import java.util.UUID;

public interface AttachmentWorkflowApi {
  AttachmentWorkflowDto createWorkflow(UUID debtorId, CreateAttachmentWorkflowRequest request);

  AttachmentWorkflowDto dispatchWorkflow(UUID debtorId, UUID workflowId);

  AttachmentWorkflowDto withdrawWorkflow(UUID debtorId, UUID workflowId, WithdrawAttachmentWorkflowRequest request);

  AttachmentWorkflowDto processCallback(UUID debtorId, AttachmentWorkflowCallbackRequest request);

  List<AttachmentWorkflowDto> getWorkflows(UUID debtorId);

  AttachmentWorkflowDto getWorkflow(UUID debtorId, UUID workflowId);
}
