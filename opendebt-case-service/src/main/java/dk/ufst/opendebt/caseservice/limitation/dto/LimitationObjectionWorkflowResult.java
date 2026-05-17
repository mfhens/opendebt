package dk.ufst.opendebt.caseservice.limitation.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitationObjectionWorkflowResult {

  private UUID indsigelsesId;
  private UUID workflowCaseId;
  private String workflowStatus;
  private String authoritativeOutcome;
  private Instant decidedAt;
}
