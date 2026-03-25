package dk.ufst.opendebt.gateway.creditor.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.gateway.creditor.client.*;
import dk.ufst.opendebt.gateway.creditor.dto.*;

@ExtendWith(MockitoExtension.class)
class CreditorIngressServiceImplTest {

  @Mock private CreditorServiceClient creditorServiceClient;
  @Mock private DebtServiceClient debtServiceClient;

  private CreditorIngressServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new CreditorIngressServiceImpl(creditorServiceClient, debtServiceClient);
  }

  @Test
  void submitClaim_resolvesAccessBeforeRouting() {
    UUID creditorOrgId = UUID.randomUUID();
    mockAllowedAccess(creditorOrgId);
    mockSuccessfulSubmission();

    service.submitClaim("SYS1", sampleRequest(), "corr-1");

    verify(creditorServiceClient)
        .resolveAccess(
            argThat(
                req ->
                    req.getChannelType() == ChannelType.M2M
                        && "SYS1".equals(req.getPresentedIdentity())
                        && req.getRequestedAction() == CreditorAction.CREATE_CLAIM));
  }

  @Test
  void submitClaim_usesResolvedCreditorIdInDebtDto() {
    UUID creditorOrgId = UUID.randomUUID();
    mockAllowedAccess(creditorOrgId);
    mockSuccessfulSubmission();

    service.submitClaim("SYS1", sampleRequest(), "corr-1");

    verify(debtServiceClient)
        .submitClaim(argThat(dto -> creditorOrgId.toString().equals(dto.getCreditorId())));
  }

  @Test
  void submitClaim_throwsWhenAccessDenied() {
    AccessResolutionResponse denied =
        AccessResolutionResponse.builder()
            .channelType(ChannelType.M2M)
            .allowed(false)
            .reasonCode("CHANNEL_NOT_BOUND")
            .build();
    when(creditorServiceClient.resolveAccess(any())).thenReturn(denied);
    ClaimSubmissionRequest request = sampleRequest();

    assertThatThrownBy(() -> service.submitClaim("SYS-BAD", request, "corr-1"))
        .isInstanceOf(OpenDebtException.class)
        .hasFieldOrPropertyWithValue("errorCode", "M2M_ACCESS_DENIED");

    verify(debtServiceClient, never()).submitClaim(any());
  }

  @Test
  void submitClaim_mapsAcceptedOutcome() {
    mockAllowedAccess(UUID.randomUUID());
    mockSuccessfulSubmission();

    GatewayClaimResponse response = service.submitClaim("SYS1", sampleRequest(), "corr-1");

    assertThat(response.getOutcome()).isEqualTo(GatewayClaimResponse.Outcome.ACCEPTED);
    assertThat(response.getCorrelationId()).isEqualTo("corr-1");
  }

  @Test
  void submitClaim_mapsRejectedOutcome() {
    mockAllowedAccess(UUID.randomUUID());
    ClaimSubmissionResult rejected =
        ClaimSubmissionResult.builder().outcome(ClaimSubmissionResult.Outcome.AFVIST).build();
    when(debtServiceClient.submitClaim(any())).thenThrow(new ClaimRejectedException(rejected));

    GatewayClaimResponse response = service.submitClaim("SYS1", sampleRequest(), "corr-2");

    assertThat(response.getOutcome()).isEqualTo(GatewayClaimResponse.Outcome.REJECTED);
  }

  @Test
  void submitClaim_mapsPendingReviewOutcome() {
    mockAllowedAccess(UUID.randomUUID());
    ClaimSubmissionResult hoering =
        ClaimSubmissionResult.builder()
            .outcome(ClaimSubmissionResult.Outcome.HOERING)
            .claimId(UUID.randomUUID())
            .build();
    when(debtServiceClient.submitClaim(any())).thenReturn(hoering);

    GatewayClaimResponse response = service.submitClaim("SYS1", sampleRequest(), "corr-3");

    assertThat(response.getOutcome()).isEqualTo(GatewayClaimResponse.Outcome.PENDING_REVIEW);
  }

  @Test
  void submitClaim_propagatesCorrelationId() {
    mockAllowedAccess(UUID.randomUUID());
    mockSuccessfulSubmission();

    GatewayClaimResponse response = service.submitClaim("SYS1", sampleRequest(), "my-trace-id-123");

    assertThat(response.getCorrelationId()).isEqualTo("my-trace-id-123");
  }

  private void mockAllowedAccess(UUID creditorOrgId) {
    AccessResolutionResponse allowed =
        AccessResolutionResponse.builder()
            .channelType(ChannelType.M2M)
            .actingCreditorOrgId(creditorOrgId)
            .allowed(true)
            .build();
    when(creditorServiceClient.resolveAccess(any())).thenReturn(allowed);
  }

  private void mockSuccessfulSubmission() {
    ClaimSubmissionResult result =
        ClaimSubmissionResult.builder()
            .outcome(ClaimSubmissionResult.Outcome.UDFOERT)
            .claimId(UUID.randomUUID())
            .caseId(UUID.randomUUID())
            .build();
    when(debtServiceClient.submitClaim(any())).thenReturn(result);
  }

  private ClaimSubmissionRequest sampleRequest() {
    return ClaimSubmissionRequest.builder()
        .debtorId("debtor-001")
        .creditorId("creditor-001")
        .debtTypeCode("SKAT")
        .principalAmount(new BigDecimal("10000.00"))
        .dueDate(LocalDate.of(2026, 6, 1))
        .build();
  }
}
