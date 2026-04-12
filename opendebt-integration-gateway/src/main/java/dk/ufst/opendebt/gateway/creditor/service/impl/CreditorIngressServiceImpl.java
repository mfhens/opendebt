package dk.ufst.opendebt.gateway.creditor.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.gateway.creditor.client.*;
import dk.ufst.opendebt.gateway.creditor.dto.*;
import dk.ufst.opendebt.gateway.creditor.service.CreditorIngressService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditorIngressServiceImpl implements CreditorIngressService {

  private final CreditorServiceClient creditorServiceClient;
  private final DebtServiceClient debtServiceClient;

  @Override
  public GatewayClaimResponse submitClaim(
      String presentedIdentity, ClaimSubmissionRequest request, String correlationId) {

    AccessResolutionResponse accessResult = resolveAndValidateAccess(presentedIdentity);

    DebtDto debtDto = mapToDebtDto(request, accessResult);

    try {
      ClaimSubmissionResult result = debtServiceClient.submitClaim(debtDto);
      return mapToGatewayResponse(result, correlationId);
    } catch (ClaimRejectedException e) {
      return mapToGatewayResponse(e.getResult(), correlationId);
    }
  }

  private AccessResolutionResponse resolveAndValidateAccess(String presentedIdentity) {
    AccessResolutionRequest accessRequest =
        AccessResolutionRequest.builder()
            .channelType(ChannelType.M2M)
            .presentedIdentity(presentedIdentity)
            .requestedAction(CreditorAction.CREATE_CLAIM)
            .build();

    AccessResolutionResponse accessResult = creditorServiceClient.resolveAccess(accessRequest);

    if (!accessResult.isAllowed()) {
      throw new OpenDebtException(
          "M2M access denied for identity: " + presentedIdentity,
          "M2M_ACCESS_DENIED",
          OpenDebtException.ErrorSeverity.WARNING);
    }

    log.info(
        "M2M access resolved: identity={} actingCreditor={}",
        presentedIdentity,
        accessResult.getActingCreditorOrgId());

    return accessResult;
  }

  private DebtDto mapToDebtDto(ClaimSubmissionRequest request, AccessResolutionResponse access) {
    return DebtDto.builder()
        .debtorId(request.getDebtorId())
        .creditorId(access.getActingCreditorOrgId().toString())
        .debtTypeCode(request.getDebtTypeCode())
        .principalAmount(request.getPrincipalAmount())
        .interestAmount(request.getInterestAmount())
        .feesAmount(request.getFeesAmount())
        .dueDate(request.getDueDate())
        .originalDueDate(request.getOriginalDueDate())
        .externalReference(request.getExternalReference())
        .ocrLine(request.getOcrLine())
        .claimArt(request.getClaimArt())
        .claimCategory(request.getClaimCategory())
        .creditorReference(request.getCreditorReference())
        .description(request.getDescription())
        .limitationDate(request.getLimitationDate())
        .periodFrom(request.getPeriodFrom())
        .periodTo(request.getPeriodTo())
        .inceptionDate(request.getInceptionDate())
        .paymentDeadline(request.getPaymentDeadline())
        .lastPaymentDate(request.getLastPaymentDate())
        .estateProcessing(request.getEstateProcessing())
        .judgmentDate(request.getJudgmentDate())
        .settlementDate(request.getSettlementDate())
        .interestRule(request.getInterestRule())
        .interestRateCode(request.getInterestRateCode())
        .additionalInterestRate(request.getAdditionalInterestRate())
        .claimNote(request.getClaimNote())
        .customerNote(request.getCustomerNote())
        .build();
  }

  private GatewayClaimResponse mapToGatewayResponse(
      ClaimSubmissionResult result, String correlationId) {
    GatewayClaimResponse.Outcome outcome =
        switch (result.getOutcome()) {
          case UDFOERT -> GatewayClaimResponse.Outcome.ACCEPTED;
          case AFVIST -> GatewayClaimResponse.Outcome.REJECTED;
          case HOERING -> GatewayClaimResponse.Outcome.PENDING_REVIEW;
        };

    List<String> errors = null;
    if (result.getErrors() != null && !result.getErrors().isEmpty()) {
      errors =
          result.getErrors().stream()
              .map(
                  e -> {
                    String code =
                        e.getRuleCode() != null && !e.getRuleCode().isBlank()
                            ? e.getRuleCode()
                            : e.getErrorCode();
                    return code != null ? code + ": " + e.getMessage() : e.getMessage();
                  })
              .collect(Collectors.toList());
    }

    return GatewayClaimResponse.builder()
        .outcome(outcome)
        .claimId(result.getClaimId())
        .caseId(result.getCaseId())
        .correlationId(correlationId)
        .errors(errors)
        .build();
  }
}
