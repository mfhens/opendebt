package dk.ufst.opendebt.debtservice.limitation.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectionDecisionRequest {

  private String outcome;
  private String rationale;
  private String decidedBy;
}
