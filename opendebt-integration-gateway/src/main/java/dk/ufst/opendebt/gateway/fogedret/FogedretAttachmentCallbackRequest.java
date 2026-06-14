package dk.ufst.opendebt.gateway.fogedret;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FogedretAttachmentCallbackRequest {
  private UUID debtorId;
  private String workflowReference;
  private String status;
  private LocalDate outcomeDate;
  private String reasonCode;
  private String callbackMessageId;
  private String externalCaseNumber;
}
