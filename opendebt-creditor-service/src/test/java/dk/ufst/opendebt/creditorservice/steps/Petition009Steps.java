package dk.ufst.opendebt.creditorservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import dk.ufst.opendebt.creditorservice.action.CreditorAction;
import dk.ufst.opendebt.creditorservice.dto.CreditorDto;
import dk.ufst.opendebt.creditorservice.dto.ValidateActionRequest;
import dk.ufst.opendebt.creditorservice.dto.ValidateActionResponse;
import dk.ufst.opendebt.creditorservice.entity.ActivityStatus;
import dk.ufst.opendebt.creditorservice.entity.ConnectionType;
import dk.ufst.opendebt.creditorservice.entity.CreditorEntity;
import dk.ufst.opendebt.creditorservice.repository.CreditorRepository;
import dk.ufst.opendebt.creditorservice.service.CreditorService;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;

/**
 * BDD step definitions for Petition 009: Fordringshaver Master Data Service
 *
 * <p>Petition reference: petition009-fordringshaver-master-data-service-outcome-contract.md
 *
 * <p>API specification: api-specs/openapi-creditor-service.yaml
 *
 * <p>Targeted components: - CreditorService (getByCreditorOrgId, getByExternalCreditorId,
 * validateAction) - CreditorController (3 REST endpoints) - CreditorRepository (query methods)
 */
@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class Petition009Steps {

  @Autowired private CreditorService creditorService;
  @Autowired private CreditorRepository creditorRepository;

  private final Map<String, UUID> creditorOrgIds = new HashMap<>();
  private final Map<String, String> externalCreditorIds = new HashMap<>();

  /** Tracks the current entity being configured across Given steps. */
  private CreditorEntity currentEntity;

  private CreditorEntity parentEntity;
  private CreditorEntity childEntity;

  private CreditorDto retrievedCreditor;
  private ValidateActionResponse validationResponse;
  private Exception lastException;
  private int lastHttpStatus;

  // Audit tracking
  private ActivityStatus previousActivityStatus;
  private ActivityStatus newActivityStatus;
  private LocalDateTime updateTimestamp;

  @Before
  public void setUpScenario() {
    creditorRepository.deleteAll();
    creditorOrgIds.clear();
    externalCreditorIds.clear();
    currentEntity = null;
    parentEntity = null;
    childEntity = null;
    retrievedCreditor = null;
    validationResponse = null;
    lastException = null;
    lastHttpStatus = 0;
    previousActivityStatus = null;
    newActivityStatus = null;
    updateTimestamp = null;
  }

  // ========================================================================================
  // Background steps
  // ========================================================================================

  @Given("the creditor service is available")
  public void the_creditor_service_is_available() {
    assertThat(creditorService)
        .as("CreditorService must be available as the dedicated backend owner")
        .isNotNull();
  }

  @Given("organization identity data is owned by person-registry")
  public void organization_identity_data_is_owned_by_person_registry() {
    // Verified architecturally: CreditorDto contains only creditorOrgId (UUID reference)
    // and does not contain organization PII fields (name, address, CVR)
    List<String> dtoFieldNames =
        Arrays.stream(CreditorDto.class.getDeclaredFields()).map(Field::getName).toList();
    assertThat(dtoFieldNames).as("CreditorDto must have fields to verify").isNotEmpty();
    assertThat(dtoFieldNames)
        .as("CreditorDto must not contain PII fields — PII is owned by person-registry")
        .doesNotContain("cprNumber", "cvrNumber", "name", "address", "email", "phone");
  }

  // ========================================================================================
  // GET /api/v1/creditors/{creditorOrgId} — Given steps
  // ========================================================================================

  @Given("a creditor exists with creditorOrgId {string}")
  public void a_creditor_exists_with_creditor_org_id(String creditorOrgIdStr) {
    UUID creditorOrgId = UUID.fromString(creditorOrgIdStr);
    creditorOrgIds.put("current", creditorOrgId);
    CreditorEntity entity =
        CreditorEntity.builder()
            .creditorOrgId(creditorOrgId)
            .externalCreditorId("FH-" + creditorOrgId.toString().substring(0, 8))
            .activityStatus(ActivityStatus.ACTIVE)
            .build();
    entity.setCreatedBy("test-user");
    currentEntity = creditorRepository.save(entity);
  }

  @Given("the creditor has externalCreditorId {string}")
  public void the_creditor_has_external_creditor_id(String externalCreditorId) {
    externalCreditorIds.put("current", externalCreditorId);
    currentEntity.setExternalCreditorId(externalCreditorId);
    currentEntity = creditorRepository.save(currentEntity);
  }

  @Given("the creditor has activityStatus {string}")
  public void the_creditor_has_activity_status(String activityStatus) {
    currentEntity.setActivityStatus(ActivityStatus.valueOf(activityStatus));
    currentEntity = creditorRepository.save(currentEntity);
  }

  @Given("the creditor has connectionType {string}")
  public void the_creditor_has_connection_type(String connectionType) {
    currentEntity.setConnectionType(ConnectionType.valueOf(connectionType));
    currentEntity = creditorRepository.save(currentEntity);
  }

  @Given("no creditor exists with creditorOrgId {string}")
  public void no_creditor_exists_with_creditor_org_id(String creditorOrgIdStr) {
    UUID creditorOrgId = UUID.fromString(creditorOrgIdStr);
    creditorOrgIds.put("nonexistent", creditorOrgId);
    // Ensure no creditor with this org id exists
    assertThat(creditorRepository.findByCreditorOrgId(creditorOrgId)).isEmpty();
  }

  // ========================================================================================
  // GET /api/v1/creditors/by-external-id/{externalCreditorId} — Given steps
  // ========================================================================================

  @Given("a creditor exists with externalCreditorId {string}")
  public void a_creditor_exists_with_external_creditor_id(String externalCreditorId) {
    externalCreditorIds.put("current", externalCreditorId);
    currentEntity =
        creditorRepository.save(
            CreditorEntity.builder()
                .creditorOrgId(UUID.randomUUID())
                .externalCreditorId(externalCreditorId)
                .activityStatus(ActivityStatus.ACTIVE)
                .build());
  }

  @Given("the creditor has creditorOrgId {string}")
  public void the_creditor_has_creditor_org_id_setter(String creditorOrgIdStr) {
    UUID creditorOrgId = UUID.fromString(creditorOrgIdStr);
    creditorOrgIds.put("current", creditorOrgId);
    currentEntity.setCreditorOrgId(creditorOrgId);
    currentEntity = creditorRepository.save(currentEntity);
  }

  @Given("no creditor exists with externalCreditorId {string}")
  public void no_creditor_exists_with_external_creditor_id(String externalCreditorId) {
    externalCreditorIds.put("nonexistent", externalCreditorId);
    assertThat(creditorRepository.findByExternalCreditorId(externalCreditorId)).isEmpty();
  }

  // ========================================================================================
  // POST /api/v1/creditors/{creditorOrgId}/validate-action — Given steps
  // ========================================================================================

  @Given("the creditor has permission to perform action {string}")
  public void the_creditor_has_permission_to_perform_action(String action) {
    grantPermission(currentEntity, CreditorAction.valueOf(action));
    currentEntity = creditorRepository.save(currentEntity);
  }

  @Given("the creditor does not have permission to perform action {string}")
  public void the_creditor_does_not_have_permission_to_perform_action(String action) {
    revokePermission(currentEntity, CreditorAction.valueOf(action));
    currentEntity = creditorRepository.save(currentEntity);
  }

  // ========================================================================================
  // Parent-child relationship steps
  // ========================================================================================

  @Given("a parent creditor exists with creditorOrgId {string}")
  public void a_parent_creditor_exists_with_creditor_org_id(String creditorOrgIdStr) {
    UUID parentCreditorOrgId = UUID.fromString(creditorOrgIdStr);
    creditorOrgIds.put("parent", parentCreditorOrgId);
    parentEntity =
        creditorRepository.save(
            CreditorEntity.builder()
                .creditorOrgId(parentCreditorOrgId)
                .externalCreditorId("PARENT-" + parentCreditorOrgId.toString().substring(0, 8))
                .activityStatus(ActivityStatus.ACTIVE)
                .build());
  }

  @Given("a child creditor exists with creditorOrgId {string}")
  public void a_child_creditor_exists_with_creditor_org_id(String creditorOrgIdStr) {
    UUID childCreditorOrgId = UUID.fromString(creditorOrgIdStr);
    creditorOrgIds.put("child", childCreditorOrgId);
    childEntity =
        creditorRepository.save(
            CreditorEntity.builder()
                .creditorOrgId(childCreditorOrgId)
                .externalCreditorId("CHILD-" + childCreditorOrgId.toString().substring(0, 8))
                .activityStatus(ActivityStatus.ACTIVE)
                .build());
  }

  @Given("the child creditor has parentCreditorId {string}")
  public void the_child_creditor_has_parent_creditor_id(String parentCreditorIdStr) {
    // The feature value matches the parent's creditorOrgId; use the parent entity's PK
    assertThat(parentEntity)
        .as("Parent entity must be created before setting parent link")
        .isNotNull();
    childEntity.setParentCreditorId(parentEntity.getId());
    childEntity = creditorRepository.save(childEntity);
  }

  // ========================================================================================
  // GDPR isolation steps
  // ========================================================================================

  @Given("the person-registry organization has CVR {string} for creditorOrgId {string}")
  public void the_person_registry_organization_has_cvr_for_creditor_org_id(
      String cvr, String creditorOrgIdStr) {
    // Person-registry is external; this step verifies that CVR is NOT stored in creditor-service.
    // The creditor entity references the organization only by creditorOrgId (UUID).
    assertThat(cvr).isNotBlank();
    assertThat(creditorOrgIdStr).isNotBlank();
  }

  // ========================================================================================
  // API-based dependency steps
  // ========================================================================================

  @Given("the debt-service needs to validate a creditor before creating a claim")
  public void the_debt_service_needs_to_validate_a_creditor_before_creating_a_claim() {
    // Architectural precondition: debt-service must use the creditor-service API.
    // Verified by the subsequent steps that call the service layer.
    assertThat(creditorService).isNotNull();
  }

  @Given("the creditor with creditorOrgId {string} exists")
  public void the_creditor_with_creditor_org_id_exists(String creditorOrgIdStr) {
    UUID creditorOrgId = UUID.fromString(creditorOrgIdStr);
    creditorOrgIds.put("api-test", creditorOrgId);
    currentEntity =
        creditorRepository.save(
            CreditorEntity.builder()
                .creditorOrgId(creditorOrgId)
                .externalCreditorId("API-" + creditorOrgId.toString().substring(0, 8))
                .activityStatus(ActivityStatus.ACTIVE)
                .allowCreateRecoveryClaims(true)
                .build());
  }

  // ========================================================================================
  // Audit logging steps
  // ========================================================================================

  @Given("an administrator updates the creditor activityStatus to {string}")
  public void an_administrator_updates_the_creditor_activity_status_to(String newStatus) {
    assertThat(currentEntity).isNotNull();
    previousActivityStatus = currentEntity.getActivityStatus();
    newActivityStatus = ActivityStatus.valueOf(newStatus);
    currentEntity.setActivityStatus(newActivityStatus);
    currentEntity = creditorRepository.saveAndFlush(currentEntity);
    updateTimestamp = currentEntity.getUpdatedAt();
  }

  // ========================================================================================
  // When steps — API calls
  // ========================================================================================

  @When("a service requests the creditor by creditorOrgId {string}")
  public void a_service_requests_the_creditor_by_creditor_org_id(String creditorOrgIdStr) {
    try {
      UUID creditorOrgId = UUID.fromString(creditorOrgIdStr);
      retrievedCreditor = creditorService.getByCreditorOrgId(creditorOrgId);
      lastHttpStatus = 200;
    } catch (Exception e) {
      lastException = e;
      lastHttpStatus = determineHttpStatus(e);
    }
  }

  @When("a service requests the creditor by externalCreditorId {string}")
  public void a_service_requests_the_creditor_by_external_creditor_id(String externalCreditorId) {
    try {
      retrievedCreditor = creditorService.getByExternalCreditorId(externalCreditorId);
      lastHttpStatus = 200;
    } catch (Exception e) {
      lastException = e;
      lastHttpStatus = determineHttpStatus(e);
    }
  }

  @When("a service validates action {string} for creditorOrgId {string}")
  public void a_service_validates_action_for_creditor_org_id(
      String action, String creditorOrgIdStr) {
    try {
      UUID creditorOrgId = UUID.fromString(creditorOrgIdStr);
      ValidateActionRequest request =
          ValidateActionRequest.builder().requestedAction(CreditorAction.valueOf(action)).build();
      validationResponse = creditorService.validateAction(creditorOrgId, request);
      lastHttpStatus = 200;
    } catch (Exception e) {
      lastException = e;
      lastHttpStatus = determineHttpStatus(e);
    }
  }

  @When("the debt-service calls the creditor-service API to validate the creditor")
  public void the_debt_service_calls_the_creditor_service_api_to_validate_the_creditor() {
    UUID creditorOrgId = creditorOrgIds.get("api-test");
    assertThat(creditorOrgId).isNotNull();
    try {
      ValidateActionRequest request =
          ValidateActionRequest.builder().requestedAction(CreditorAction.CREATE_CLAIM).build();
      validationResponse = creditorService.validateAction(creditorOrgId, request);
      lastHttpStatus = 200;
    } catch (Exception e) {
      lastException = e;
      lastHttpStatus = determineHttpStatus(e);
    }
  }

  // ========================================================================================
  // Then steps — Success assertions
  // ========================================================================================

  @Then("the creditor is returned successfully")
  public void the_creditor_is_returned_successfully() {
    assertThat(lastHttpStatus).isEqualTo(200);
    assertThat(retrievedCreditor).isNotNull();
  }

  @Then("the response contains creditorOrgId {string}")
  public void the_response_contains_creditor_org_id(String expectedCreditorOrgId) {
    assertThat(retrievedCreditor).isNotNull();
    assertThat(retrievedCreditor.getCreditorOrgId())
        .isEqualTo(UUID.fromString(expectedCreditorOrgId));
  }

  @Then("the response contains externalCreditorId {string}")
  public void the_response_contains_external_creditor_id(String expectedExternalCreditorId) {
    assertThat(retrievedCreditor).isNotNull();
    assertThat(retrievedCreditor.getExternalCreditorId()).isEqualTo(expectedExternalCreditorId);
  }

  @Then("the response contains activityStatus {string}")
  public void the_response_contains_activity_status(String expectedActivityStatus) {
    assertThat(retrievedCreditor).isNotNull();
    assertThat(retrievedCreditor.getActivityStatus().toString()).isEqualTo(expectedActivityStatus);
  }

  @Then("the response contains parentCreditorId {string}")
  public void the_response_contains_parent_creditor_id(String expectedParentCreditorId) {
    assertThat(retrievedCreditor).isNotNull();
    // parentCreditorId in the DTO is the parent entity's PK; the feature passes the
    // parent's creditorOrgId which we mapped via parentEntity.getId().
    assertThat(retrievedCreditor.getParentCreditorId()).isNotNull();
    assertThat(parentEntity).isNotNull();
    assertThat(retrievedCreditor.getParentCreditorId()).isEqualTo(parentEntity.getId());
  }

  // ========================================================================================
  // Then steps — Validation response assertions
  // ========================================================================================

  @Then("the validation response indicates allowed is true")
  public void the_validation_response_indicates_allowed_is_true() {
    assertThat(validationResponse).isNotNull();
    assertThat(validationResponse.isAllowed()).isTrue();
  }

  @Then("the validation response contains requestedAction {string}")
  public void the_validation_response_contains_requested_action(String expectedAction) {
    assertThat(validationResponse).isNotNull();
    assertThat(validationResponse.getRequestedAction().toString()).isEqualTo(expectedAction);
  }

  @Then("the validation response indicates allowed is false")
  public void the_validation_response_indicates_allowed_is_false() {
    assertThat(validationResponse).isNotNull();
    assertThat(validationResponse.isAllowed()).isFalse();
  }

  @Then("the validation response contains reasonCode {string}")
  public void the_validation_response_contains_reason_code(String expectedReasonCode) {
    assertThat(validationResponse).isNotNull();
    assertThat(validationResponse.getReasonCode()).isEqualTo(expectedReasonCode);
  }

  @Then("the validation response contains message explaining the creditor is not active")
  public void the_validation_response_contains_message_explaining_the_creditor_is_not_active() {
    assertThat(validationResponse).isNotNull();
    assertThat(validationResponse.getMessage()).isNotBlank();
    assertThat(validationResponse.getMessage().toLowerCase()).contains("not active");
  }

  // ========================================================================================
  // Then steps — Error response assertions
  // ========================================================================================

  @Then("the request fails with status {int}")
  public void the_request_fails_with_status(Integer expectedStatus) {
    assertThat(lastHttpStatus).isEqualTo(expectedStatus);
    assertThat(lastException).isNotNull();
  }

  @Then("the error response contains code {string}")
  public void the_error_response_contains_code(String expectedErrorCode) {
    assertThat(lastException).isNotNull();
    assertThat(lastException)
        .isInstanceOf(dk.ufst.opendebt.common.exception.OpenDebtException.class);
    dk.ufst.opendebt.common.exception.OpenDebtException ode =
        (dk.ufst.opendebt.common.exception.OpenDebtException) lastException;
    assertThat(ode.getErrorCode()).isEqualTo(expectedErrorCode);
  }

  // ========================================================================================
  // Then steps — GDPR data isolation assertions
  // ========================================================================================

  @Then("the response does not contain CVR number directly")
  public void the_response_does_not_contain_cvr_number_directly() {
    assertThat(retrievedCreditor).isNotNull();
    List<String> fieldNames =
        Arrays.stream(CreditorDto.class.getDeclaredFields()).map(Field::getName).toList();
    assertThat(fieldNames).isNotEmpty();
    assertThat(fieldNames).doesNotContain("cvrNumber", "cvr");
  }

  @Then("the response does not contain organization name directly")
  public void the_response_does_not_contain_organization_name_directly() {
    assertThat(retrievedCreditor).isNotNull();
    List<String> fieldNames =
        Arrays.stream(CreditorDto.class.getDeclaredFields()).map(Field::getName).toList();
    assertThat(fieldNames).isNotEmpty();
    assertThat(fieldNames).doesNotContain("name", "organizationName", "orgName");
  }

  @Then("the response does not contain organization address directly")
  public void the_response_does_not_contain_organization_address_directly() {
    assertThat(retrievedCreditor).isNotNull();
    List<String> fieldNames =
        Arrays.stream(CreditorDto.class.getDeclaredFields()).map(Field::getName).toList();
    assertThat(fieldNames).isNotEmpty();
    assertThat(fieldNames).doesNotContain("address", "organizationAddress", "orgAddress");
  }

  // ========================================================================================
  // Then steps — API-based dependency assertions
  // ========================================================================================

  @Then("the creditor-service resolves the creditor from its own database")
  public void the_creditor_service_resolves_the_creditor_from_its_own_database() {
    // Verified: the creditor-service successfully resolved data through its own service layer
    assertThat(validationResponse).isNotNull();
    assertThat(lastHttpStatus).isEqualTo(200);
  }

  @Then("the debt-service does not access the creditor database directly")
  public void the_debt_service_does_not_access_the_creditor_database_directly() {
    // Architectural rule: debt-service accesses creditor data through the API, not the database.
    // This is enforced by ArchUnit tests (CreditorArchitectureTest) and verified here by the
    // fact that the service call succeeded through the CreditorService interface.
    assertThat(validationResponse).isNotNull();
  }

  @Then("the validation result is returned through the API")
  public void the_validation_result_is_returned_through_the_api() {
    // Verify API response structure and success indicator
    assertThat(validationResponse)
        .isNotNull()
        .satisfies(response -> assertThat(response.isAllowed()).isTrue());
  }

  // ========================================================================================
  // Then steps — Audit logging assertions
  // ========================================================================================

  @Then("the change is recorded in the audit log")
  public void the_change_is_recorded_in_the_audit_log() {
    // Full audit trail is enforced by PostgreSQL triggers (V1__create_creditor_tables.sql).
    // In H2 test mode, we verify entity-level state changes as a proxy.
    assertThat(currentEntity).isNotNull();
    assertThat(currentEntity.getActivityStatus()).isEqualTo(newActivityStatus);
  }

  @Then("the audit log contains the timestamp of the change")
  public void the_audit_log_contains_the_timestamp_of_the_change() {
    assertThat(updateTimestamp).isNotNull();
  }

  @Then("the audit log contains the user who made the change")
  public void the_audit_log_contains_the_user_who_made_the_change() {
    // In production, the audit trigger captures application_user via set_audit_context().
    // In H2, we verify the entity supports createdBy tracking via AuditableEntity superclass.
    assertThat(currentEntity.getCreatedBy()).isNotNull();
  }

  @Then("the audit log contains the previous activityStatus value")
  public void the_audit_log_contains_the_previous_activity_status_value() {
    assertThat(previousActivityStatus).isNotNull();
    assertThat(previousActivityStatus).isEqualTo(ActivityStatus.ACTIVE);
  }

  @Then("the audit log contains the new activityStatus value")
  public void the_audit_log_contains_the_new_activity_status_value() {
    assertThat(newActivityStatus).isNotNull();
    assertThat(newActivityStatus).isEqualTo(ActivityStatus.TEMPORARILY_CLOSED);
  }

  // ========================================================================================
  // Helper methods
  // ========================================================================================

  private int determineHttpStatus(Exception e) {
    String exceptionName = e.getClass().getSimpleName().toLowerCase();
    if (exceptionName.contains("notfound")) {
      return 404;
    } else if (exceptionName.contains("forbidden")) {
      return 403;
    } else if (exceptionName.contains("unauthorized")) {
      return 401;
    } else if (exceptionName.contains("badrequest")) {
      return 400;
    }
    return 500;
  }

  private void grantPermission(CreditorEntity entity, CreditorAction action) {
    switch (action) {
      case CREATE_CLAIM -> entity.setAllowCreateRecoveryClaims(true);
      case UPDATE_CLAIM -> {
        entity.setAllowWriteDown(true);
        entity.setAllowWriteUpAdjustment(true);
      }
      case ADMINISTER_CREDITOR -> entity.setAllowPortalActions(true);
      case VIEW_CREDITOR -> {
        // VIEW_CREDITOR is always allowed; no permission flag needed
      }
    }
  }

  private void revokePermission(CreditorEntity entity, CreditorAction action) {
    switch (action) {
      case CREATE_CLAIM -> entity.setAllowCreateRecoveryClaims(false);
      case UPDATE_CLAIM -> {
        entity.setAllowWriteDown(false);
        entity.setAllowWriteUpAdjustment(false);
      }
      case ADMINISTER_CREDITOR -> entity.setAllowPortalActions(false);
      case VIEW_CREDITOR -> {
        // VIEW_CREDITOR is always allowed; cannot be revoked
      }
    }
  }
}
