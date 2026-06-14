package dk.ufst.opendebt.debtservice.attachment;

public interface AttachmentCallbackValidator {
  void validate(AttachmentWorkflowDto workflow, AttachmentWorkflowCallbackRequest request);
}
