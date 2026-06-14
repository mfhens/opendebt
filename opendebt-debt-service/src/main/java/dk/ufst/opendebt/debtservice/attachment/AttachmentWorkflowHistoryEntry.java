package dk.ufst.opendebt.debtservice.attachment;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentWorkflowHistoryEntry {
  private AttachmentWorkflowStatus status;
  private OffsetDateTime recordedAt;
  private String note;
  private LocalDate outcomeDate;
}
