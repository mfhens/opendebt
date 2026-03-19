package dk.ufst.opendebt.gateway.creditor.client;

import lombok.Getter;

@Getter
public class ClaimRejectedException extends RuntimeException {

  private final ClaimSubmissionResult result;

  public ClaimRejectedException(ClaimSubmissionResult result) {
    super("Claim rejected by debt-service");
    this.result = result;
  }
}
