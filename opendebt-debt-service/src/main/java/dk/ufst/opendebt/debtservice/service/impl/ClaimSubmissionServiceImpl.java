package dk.ufst.opendebt.debtservice.service.impl;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.debtservice.client.CaseServiceClient;
import dk.ufst.opendebt.debtservice.dto.ClaimSubmissionResponse;
import dk.ufst.opendebt.debtservice.dto.ClaimValidationResult;
import dk.ufst.opendebt.debtservice.service.ClaimSubmissionService;
import dk.ufst.opendebt.debtservice.service.ClaimValidationContext;
import dk.ufst.opendebt.debtservice.service.ClaimValidationService;
import dk.ufst.opendebt.debtservice.service.DebtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimSubmissionServiceImpl implements ClaimSubmissionService {

  private final ClaimValidationService validationService;
  private final DebtService debtService;
  private final CaseServiceClient caseServiceClient;

  @Override
  @Transactional
  public ClaimSubmissionResponse submitClaim(DebtDto claim, ClaimValidationContext context) {
    log.info(
        "Submitting claim: debtorId={}, type={}, amount={}, ingressPath={}",
        claim.getDebtorId(),
        claim.getDebtTypeCode(),
        claim.getPrincipalAmount(),
        context.ingressPath());

    ClaimValidationResult validationResult = validationService.validate(claim, context);

    if (!validationResult.isValid()) {
      log.info("Claim rejected with {} validation errors", validationResult.getErrors().size());
      return ClaimSubmissionResponse.builder()
          .outcome(ClaimSubmissionResponse.Outcome.AFVIST)
          .errors(validationResult.getErrors())
          .build();
    }

    // Set lifecycle state and status for accepted claims
    claim.setLifecycleState("OVERDRAGET");

    DebtDto created = debtService.createDebt(claim);

    // Auto-assign to case
    try {
      var caseAssignment =
          caseServiceClient.assignDebtToCase(created.getId().toString(), claim.getDebtorId());
      log.info(
          "Claim {} assigned to case {}",
          created.getId(),
          caseAssignment != null ? caseAssignment.getCaseId() : "unknown");

      return ClaimSubmissionResponse.builder()
          .outcome(ClaimSubmissionResponse.Outcome.UDFOERT)
          .claimId(created.getId())
          .caseId(caseAssignment != null ? caseAssignment.getCaseId() : null)
          .errors(Collections.emptyList())
          .build();
    } catch (Exception ex) {
      log.warn("Claim {} created but case assignment failed: {}", created.getId(), ex.getMessage());
      return ClaimSubmissionResponse.builder()
          .outcome(ClaimSubmissionResponse.Outcome.UDFOERT)
          .claimId(created.getId())
          .errors(Collections.emptyList())
          .build();
    }
  }
}
