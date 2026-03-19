package dk.ufst.opendebt.gateway.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.gateway.creditor.client.*;
import dk.ufst.opendebt.gateway.creditor.dto.*;
import dk.ufst.opendebt.gateway.creditor.service.CreditorIngressService;
import dk.ufst.opendebt.gateway.creditor.service.impl.CreditorIngressServiceImpl;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition011Steps {

  private CreditorServiceClient creditorServiceClient;
  private DebtServiceClient debtServiceClient;
  private CreditorIngressService ingressService;

  private String currentIdentity;
  private UUID resolvedCreditorOrgId;
  private GatewayClaimResponse lastResponse;
  private Exception lastException;
  private boolean debtServiceCalled;
  private String lastCorrelationId;

  @Before
  public void setUp() {
    creditorServiceClient = mock(CreditorServiceClient.class);
    debtServiceClient = mock(DebtServiceClient.class);
    ingressService = new CreditorIngressServiceImpl(creditorServiceClient, debtServiceClient);
    lastResponse = null;
    lastException = null;
    debtServiceCalled = false;
    lastCorrelationId = null;
  }

  @Given("creditor system {string} is authorized for fordringshaver {string}")
  public void creditorSystemIsAuthorized(String systemId, String creditorKey) {
    currentIdentity = systemId;
    resolvedCreditorOrgId = UUID.nameUUIDFromBytes(creditorKey.getBytes());

    AccessResolutionResponse accessResponse =
        AccessResolutionResponse.builder()
            .channelType(ChannelType.M2M)
            .actingCreditorOrgId(resolvedCreditorOrgId)
            .allowed(true)
            .build();
    when(creditorServiceClient.resolveAccess(any())).thenReturn(accessResponse);

    ClaimSubmissionResult submissionResult =
        ClaimSubmissionResult.builder()
            .outcome(ClaimSubmissionResult.Outcome.UDFOERT)
            .claimId(UUID.randomUUID())
            .caseId(UUID.randomUUID())
            .build();
    when(debtServiceClient.submitClaim(any()))
        .thenAnswer(
            invocation -> {
              debtServiceCalled = true;
              return submissionResult;
            });
  }

  @Given("creditor system {string} is not authorized for the requested operation")
  public void creditorSystemIsNotAuthorized(String systemId) {
    currentIdentity = systemId;

    AccessResolutionResponse accessResponse =
        AccessResolutionResponse.builder()
            .channelType(ChannelType.M2M)
            .allowed(false)
            .reasonCode("CHANNEL_NOT_BOUND")
            .message("System is not bound to any creditor")
            .build();
    when(creditorServiceClient.resolveAccess(any())).thenReturn(accessResponse);
  }

  @When("system {string} submits a fordring through DUPLA and integration-gateway")
  public void systemSubmitsFordring(String systemId) {
    lastCorrelationId = UUID.randomUUID().toString();
    ClaimSubmissionRequest request = sampleClaimRequest();
    try {
      lastResponse = ingressService.submitClaim(systemId, request, lastCorrelationId);
    } catch (Exception e) {
      lastException = e;
    }
  }

  @When("system {string} calls the creditor M2M API")
  public void systemCallsM2mApi(String systemId) {
    lastCorrelationId = UUID.randomUUID().toString();
    ClaimSubmissionRequest request = sampleClaimRequest();
    try {
      lastResponse = ingressService.submitClaim(systemId, request, lastCorrelationId);
    } catch (Exception e) {
      lastException = e;
    }
  }

  @When("system {string} submits a request through integration-gateway")
  public void systemSubmitsRequest(String systemId) {
    lastCorrelationId = "TRACE-" + UUID.randomUUID();
    ClaimSubmissionRequest request = sampleClaimRequest();
    try {
      lastResponse = ingressService.submitClaim(systemId, request, lastCorrelationId);
    } catch (Exception e) {
      lastException = e;
    }
  }

  @Then("integration-gateway resolves acting fordringshaver {string}")
  public void gatewayResolvesCreditor(String creditorKey) {
    assertThat(lastException).isNull();
    assertThat(lastResponse).isNotNull();
    verify(creditorServiceClient).resolveAccess(any(AccessResolutionRequest.class));
  }

  @And("the request is routed to debt-service")
  public void requestIsRoutedToDebtService() {
    assertThat(debtServiceCalled).isTrue();
    assertThat(lastResponse.getOutcome()).isEqualTo(GatewayClaimResponse.Outcome.ACCEPTED);
  }

  @Then("integration-gateway rejects the request")
  public void gatewayRejectsRequest() {
    assertThat(lastException).isNotNull();
    assertThat(lastException).isInstanceOf(OpenDebtException.class);
    OpenDebtException ode = (OpenDebtException) lastException;
    assertThat(ode.getErrorCode()).isEqualTo("M2M_ACCESS_DENIED");
  }

  @And("the request is not routed to debt-service")
  public void requestIsNotRouted() {
    verify(debtServiceClient, never()).submitClaim(any());
  }

  @Then("the routed request contains correlation and audit context")
  public void requestContainsCorrelationContext() {
    assertThat(lastException).isNull();
    assertThat(lastResponse).isNotNull();
    assertThat(lastResponse.getCorrelationId()).isEqualTo(lastCorrelationId);
  }

  private ClaimSubmissionRequest sampleClaimRequest() {
    return ClaimSubmissionRequest.builder()
        .debtorId("test-debtor-001")
        .creditorId("test-creditor-001")
        .debtTypeCode("SKAT")
        .principalAmount(new BigDecimal("10000.00"))
        .dueDate(LocalDate.of(2026, 6, 1))
        .build();
  }
}
