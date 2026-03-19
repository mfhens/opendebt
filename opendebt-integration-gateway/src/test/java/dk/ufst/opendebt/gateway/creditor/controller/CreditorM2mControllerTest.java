package dk.ufst.opendebt.gateway.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.gateway.creditor.dto.*;
import dk.ufst.opendebt.gateway.creditor.service.CreditorIngressService;

@ExtendWith(MockitoExtension.class)
class CreditorM2mControllerTest {

  @Mock private CreditorIngressService creditorIngressService;

  private CreditorM2mController controller;

  @BeforeEach
  void setUp() {
    controller = new CreditorM2mController(creditorIngressService);
  }

  @Test
  void submitClaim_returnsCreated_whenAccepted() {
    ClaimSubmissionRequest request = sampleRequest();
    GatewayClaimResponse response =
        GatewayClaimResponse.builder()
            .outcome(GatewayClaimResponse.Outcome.ACCEPTED)
            .claimId(UUID.randomUUID())
            .caseId(UUID.randomUUID())
            .correlationId("corr-1")
            .build();
    when(creditorIngressService.submitClaim(eq("SYS1"), any(), anyString())).thenReturn(response);

    var result = controller.submitClaim("SYS1", "corr-1", request);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(result.getBody().getOutcome()).isEqualTo(GatewayClaimResponse.Outcome.ACCEPTED);
  }

  @Test
  void submitClaim_returnsUnprocessable_whenRejected() {
    ClaimSubmissionRequest request = sampleRequest();
    GatewayClaimResponse response =
        GatewayClaimResponse.builder()
            .outcome(GatewayClaimResponse.Outcome.REJECTED)
            .errors(List.of("RULE_001: Missing debtor"))
            .correlationId("corr-2")
            .build();
    when(creditorIngressService.submitClaim(eq("SYS1"), any(), anyString())).thenReturn(response);

    var result = controller.submitClaim("SYS1", "corr-2", request);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(result.getBody().getOutcome()).isEqualTo(GatewayClaimResponse.Outcome.REJECTED);
  }

  @Test
  void submitClaim_returnsCreated_whenPendingReview() {
    ClaimSubmissionRequest request = sampleRequest();
    GatewayClaimResponse response =
        GatewayClaimResponse.builder()
            .outcome(GatewayClaimResponse.Outcome.PENDING_REVIEW)
            .claimId(UUID.randomUUID())
            .correlationId("corr-3")
            .build();
    when(creditorIngressService.submitClaim(eq("SYS1"), any(), anyString())).thenReturn(response);

    var result = controller.submitClaim("SYS1", "corr-3", request);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(result.getBody().getOutcome())
        .isEqualTo(GatewayClaimResponse.Outcome.PENDING_REVIEW);
  }

  @Test
  void submitClaim_generatesCorrelationId_whenNotProvided() {
    ClaimSubmissionRequest request = sampleRequest();
    GatewayClaimResponse response =
        GatewayClaimResponse.builder()
            .outcome(GatewayClaimResponse.Outcome.ACCEPTED)
            .claimId(UUID.randomUUID())
            .build();
    when(creditorIngressService.submitClaim(eq("SYS1"), any(), anyString())).thenReturn(response);

    var result = controller.submitClaim("SYS1", null, request);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    verify(creditorIngressService).submitClaim(eq("SYS1"), any(), anyString());
  }

  @Test
  void handleOpenDebtException_returnsForbidden_forAccessDenied() {
    OpenDebtException exception =
        new OpenDebtException(
            "M2M access denied", "M2M_ACCESS_DENIED", OpenDebtException.ErrorSeverity.WARNING);

    var result = controller.handleOpenDebtException(exception);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(result.getBody().getErrorCode()).isEqualTo("M2M_ACCESS_DENIED");
  }

  @Test
  void handleOpenDebtException_returnsBadGateway_forServiceError() {
    OpenDebtException exception =
        new OpenDebtException(
            "Service down", "DEBT_SERVICE_UNAVAILABLE", OpenDebtException.ErrorSeverity.CRITICAL);

    var result = controller.handleOpenDebtException(exception);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(result.getBody().getErrorCode()).isEqualTo("DEBT_SERVICE_UNAVAILABLE");
  }

  private ClaimSubmissionRequest sampleRequest() {
    return ClaimSubmissionRequest.builder()
        .debtorId("debtor-001")
        .creditorId("creditor-001")
        .debtTypeCode("SKAT")
        .principalAmount(new BigDecimal("5000.00"))
        .dueDate(LocalDate.of(2026, 6, 1))
        .build();
  }
}
