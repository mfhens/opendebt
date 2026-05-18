package dk.ufst.opendebt.caseservice.limitation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitationObjectionDecisionRequest {

  private String outcome;
  private String rationale;
  private String decidedBy;
}
