package dk.ufst.opendebt.gateway.creditor.dto;

import java.util.List;
import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayClaimResponse {

  private Outcome outcome;
  private UUID claimId;
  private UUID caseId;
  private String correlationId;
  private List<String> errors;

  public enum Outcome {
    ACCEPTED,
    REJECTED,
    PENDING_REVIEW
  }
}
