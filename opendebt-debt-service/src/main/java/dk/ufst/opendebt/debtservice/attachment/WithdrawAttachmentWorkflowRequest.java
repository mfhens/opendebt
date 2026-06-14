package dk.ufst.opendebt.debtservice.attachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawAttachmentWorkflowRequest {
  private String reason;
}
