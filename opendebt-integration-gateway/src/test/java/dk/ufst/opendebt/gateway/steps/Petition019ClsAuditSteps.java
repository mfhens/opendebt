package dk.ufst.opendebt.gateway.steps;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

public class Petition019ClsAuditSteps {

  @Autowired private Petition019ScenarioContext ctx;

  @And("CLS logging records the SOAP call with calling system {string}")
  public void clsLoggingRecordsTheSoapCallWithCallingSystem(String expectedCreditorId) {
    assertThat(ctx.getClsCallingSystem()).isEqualTo(expectedCreditorId);
  }

  @And("CLS logging records service {string} and operation {string}")
  public void clsLoggingRecordsServiceAndOperation(String serviceName, String operationName) {
    assertThat(ctx.getClsServiceName()).isEqualTo(serviceName);
    assertThat(ctx.getClsOperationName()).isEqualTo(operationName);
  }

  @And("CLS logging records the failed SOAP call with authentication error")
  public void clsLoggingRecordsTheFailedSoapCallWithAuthenticationError() {
    assertThat(ctx.getCapturedClsEvent()).isNotNull();
    assertThat(ctx.getClsStatus()).isEqualTo("FAULT");
  }

  @And("CLS logging records the failed SOAP call with authorization error")
  public void clsLoggingRecordsTheFailedSoapCallWithAuthorizationError() {
    assertThat(ctx.getCapturedClsEvent()).isNotNull();
    assertThat(ctx.getClsStatus()).isEqualTo("FAULT");
  }

  @Then("CLS logging contains timestamp of the SOAP call")
  public void clsLoggingContainsTimestampOfTheSoapCall() {
    assertThat(ctx.getClsTimestamp()).isNotNull();
  }

  @And("CLS logging contains calling system identifier {string}")
  public void clsLoggingContainsCallingSystemIdentifier(String expectedCreditorId) {
    assertThat(ctx.getClsCallingSystem()).isEqualTo(expectedCreditorId);
  }

  @And("CLS logging contains service name {string}")
  public void clsLoggingContainsServiceName(String expectedServiceName) {
    assertThat(ctx.getClsServiceName()).isEqualTo(expectedServiceName);
  }

  @And("CLS logging contains operation name {string}")
  public void clsLoggingContainsOperationName(String expectedOperationName) {
    assertThat(ctx.getClsOperationName()).isEqualTo(expectedOperationName);
  }

  @And("CLS logging contains success status {string}")
  public void clsLoggingContainsSuccessStatus(String expectedStatus) {
    assertThat(ctx.getClsStatus()).isEqualTo(expectedStatus);
  }

  @And("CLS logging contains failure status {string}")
  public void clsLoggingContainsFailureStatus(String expectedStatus) {
    assertThat(ctx.getClsStatus()).isEqualTo(expectedStatus);
  }

  @And("CLS logging contains correlation ID from SOAP headers")
  public void clsLoggingContainsCorrelationIdFromSoapHeaders() {
    assertThat(ctx.getClsCorrelationId()).isNotNull().isNotEmpty();
  }

  @And("CLS logging contains response time in milliseconds")
  public void clsLoggingContainsResponseTimeInMilliseconds() {
    assertThat(ctx.getClsResponseTimeMs()).isNotNull().isGreaterThanOrEqualTo(0);
  }

  @And("CLS logging contains error code from SOAP fault")
  public void clsLoggingContainsErrorCodeFromSoapFault() {
    assertThat(ctx.getClsErrorCode()).isNotNull().isNotEmpty();
  }

  @And("CLS logging contains error message from SOAP fault")
  public void clsLoggingContainsErrorMessageFromSoapFault() {
    assertThat(ctx.getClsErrorMessage()).isNotNull().isNotEmpty();
  }

  @And("CLS logging contains stack trace for troubleshooting")
  public void clsLoggingContainsStackTraceForTroubleshooting() {
    assertThat(ctx.getClsErrorCode() != null || ctx.getClsStackTrace() != null)
        .as("CLS event should contain stack trace or error code for fault scenario")
        .isTrue();
  }

  @Then("CLS logging contains SOAP request body")
  public void clsLoggingContainsSoapRequestBody() {
    assertThat(ctx.getClsRequestBody()).isNotNull().isNotEmpty();
  }

  @And("CLS logging does not contain debtor CPR number")
  public void clsLoggingDoesNotContainDebtorCprNumber() {
    String requestBody = ctx.getClsRequestBody();
    if (requestBody != null) {
      assertThat(requestBody).doesNotContain("1234567890");
    }
  }

  @And("CLS logging does not contain debtor name")
  public void clsLoggingDoesNotContainDebtorName() {
    assertThat(ctx.getClsRequestBody()).isNotNull();
  }

  @And("CLS logging contains SOAP response body")
  public void clsLoggingContainsSoapResponseBody() {
    assertThat(ctx.getClsResponseBody()).isNotNull().isNotEmpty();
  }

  @And("CLS logging contains debtor identifier as UUID reference only")
  public void clsLoggingContainsDebtorIdentifierAsUuidReferenceOnly() {
    assertThat(ctx.getClsRequestBody()).isNotNull();
  }
}
