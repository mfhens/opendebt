package dk.ufst.opendebt.debtservice.attachment;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentWorkflowCallbackRequest {
  private String workflowReference;
  private String status;
  private LocalDate outcomeDate;
  private String reasonCode;
  private String callbackMessageId;
  private String externalCaseNumber;
  private String legalReference;
}
