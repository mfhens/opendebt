package dk.ufst.opendebt.gateway.creditor.service;

import dk.ufst.opendebt.gateway.creditor.dto.ClaimSubmissionRequest;
import dk.ufst.opendebt.gateway.creditor.dto.GatewayClaimResponse;

public interface CreditorIngressService {

  GatewayClaimResponse submitClaim(
      String presentedIdentity, ClaimSubmissionRequest request, String correlationId);
}
