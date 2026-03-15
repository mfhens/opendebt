package dk.ufst.opendebt.creditorservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import dk.ufst.opendebt.creditorservice.dto.*;
import dk.ufst.opendebt.creditorservice.entity.ActivityStatus;
import dk.ufst.opendebt.creditorservice.entity.ChannelBindingEntity;
import dk.ufst.opendebt.creditorservice.entity.CreditorEntity;
import dk.ufst.opendebt.creditorservice.repository.ChannelBindingRepository;
import dk.ufst.opendebt.creditorservice.repository.CreditorRepository;
import dk.ufst.opendebt.creditorservice.service.ChannelBindingService;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * BDD step definitions for Petition 010: Fordringshaver channel binding and access resolution
 *
 * <p>Petition reference:
 * petition010-fordringshaver-channel-binding-and-access-resolution-outcome-contract.md
 *
 * <p>API specification: api-specs/openapi-creditor-service.yaml (POST
 * /api/v1/creditors/access/resolve)
 *
 * <p>Targeted components: - ChannelBindingService (resolveAccess) - AccessResolutionController
 * (REST endpoint) - ChannelBindingRepository (query methods)
 */
public class Petition010Steps {

  @Autowired private ChannelBindingService channelBindingService;
  @Autowired private ChannelBindingRepository channelBindingRepository;
  @Autowired private CreditorRepository creditorRepository;

  private final Map<String, CreditorEntity> creditorsByExternalId = new HashMap<>();
  private AccessResolutionResponse lastResponse;

  @Before
  public void setUpPetition010() {
    channelBindingRepository.deleteAll();
    creditorRepository.deleteAll();
    creditorsByExternalId.clear();
    lastResponse = null;
  }

  // ========================================================================================
  // Given steps
  // ========================================================================================

  @Given("M2M identity {string} is bound to fordringshaver {string}")
  public void m2m_identity_is_bound_to_fordringshaver(String identity, String creditorExtId) {
    CreditorEntity creditor = ensureCreditor(creditorExtId);
    ChannelBindingEntity binding =
        ChannelBindingEntity.builder()
            .channelIdentity(identity)
            .channelType(ChannelType.M2M)
            .creditorId(creditor.getId())
            .active(true)
            .description("BDD test M2M binding for " + creditorExtId)
            .build();
    channelBindingRepository.save(binding);
  }

  @Given("fordringshaver {string} may act on behalf of fordringshaver {string}")
  public void fordringshaver_may_act_on_behalf_of(String parentExtId, String childExtId) {
    CreditorEntity parent = ensureCreditor(parentExtId);
    CreditorEntity child = ensureCreditor(childExtId);
    child.setParentCreditorId(parent.getId());
    creditorRepository.save(child);
  }

  @Given("portal user {string} is bound to fordringshaver {string}")
  public void portal_user_is_bound_to_fordringshaver(String userId, String creditorExtId) {
    CreditorEntity creditor = ensureCreditor(creditorExtId);
    ChannelBindingEntity binding =
        ChannelBindingEntity.builder()
            .channelIdentity(userId)
            .channelType(ChannelType.PORTAL)
            .creditorId(creditor.getId())
            .active(true)
            .description("BDD test portal binding for " + creditorExtId)
            .build();
    channelBindingRepository.save(binding);
  }

  @Given("identity {string} is not bound to any fordringshaver")
  public void identity_is_not_bound_to_any_fordringshaver(String identity) {
    // Ensure no binding exists for this identity
    assertThat(channelBindingRepository.findByChannelIdentityAndActiveTrue(identity)).isEmpty();
  }

  // ========================================================================================
  // When steps
  // ========================================================================================

  @When("OpenDebt resolves access for identity {string}")
  public void opendebt_resolves_access_for_identity(String identity) {
    AccessResolutionRequest request =
        AccessResolutionRequest.builder()
            .channelType(ChannelType.M2M)
            .presentedIdentity(identity)
            .build();
    lastResponse = channelBindingService.resolveAccess(request);
  }

  @When("OpenDebt resolves access for user {string} acting for fordringshaver {string}")
  public void opendebt_resolves_access_for_user_acting_for_fordringshaver(
      String userId, String representedExtId) {
    CreditorEntity representedCreditor = creditorsByExternalId.get(representedExtId);
    assertThat(representedCreditor)
        .as("Represented creditor %s must exist", representedExtId)
        .isNotNull();

    AccessResolutionRequest request =
        AccessResolutionRequest.builder()
            .channelType(ChannelType.PORTAL)
            .presentedIdentity(userId)
            .representedCreditorOrgId(representedCreditor.getCreditorOrgId())
            .build();
    lastResponse = channelBindingService.resolveAccess(request);
  }

  // ========================================================================================
  // Then steps
  // ========================================================================================

  @Then("the acting fordringshaver is {string}")
  public void the_acting_fordringshaver_is(String expectedExtId) {
    assertThat(lastResponse).as("Access resolution response must not be null").isNotNull();
    assertThat(lastResponse.isAllowed()).isTrue();
    CreditorEntity expectedCreditor = creditorsByExternalId.get(expectedExtId);
    assertThat(expectedCreditor).as("Expected creditor %s must exist", expectedExtId).isNotNull();
    assertThat(lastResponse.getActingCreditorOrgId())
        .isEqualTo(expectedCreditor.getCreditorOrgId());
  }

  @Then("the represented fordringshaver is {string}")
  public void the_represented_fordringshaver_is(String expectedExtId) {
    assertThat(lastResponse).as("Access resolution response must not be null").isNotNull();
    CreditorEntity expectedCreditor = creditorsByExternalId.get(expectedExtId);
    assertThat(expectedCreditor).as("Expected creditor %s must exist", expectedExtId).isNotNull();
    assertThat(lastResponse.getRepresentedCreditorOrgId())
        .isEqualTo(expectedCreditor.getCreditorOrgId());
  }

  @And("the request is allowed")
  public void the_request_is_allowed() {
    assertThat(lastResponse).as("Access resolution response must not be null").isNotNull();
    assertThat(lastResponse.isAllowed()).isTrue();
  }

  @Then("the request is rejected")
  public void the_request_is_rejected() {
    assertThat(lastResponse).as("Access resolution response must not be null").isNotNull();
    assertThat(lastResponse.isAllowed()).isFalse();
    assertThat(lastResponse.getReasonCode()).isNotBlank();
  }

  // ========================================================================================
  // Helper methods
  // ========================================================================================

  /**
   * Ensures a creditor entity exists for the given external ID. Creates it if it doesn't already
   * exist.
   */
  private CreditorEntity ensureCreditor(String externalCreditorId) {
    if (creditorsByExternalId.containsKey(externalCreditorId)) {
      return creditorsByExternalId.get(externalCreditorId);
    }
    CreditorEntity entity =
        CreditorEntity.builder()
            .creditorOrgId(UUID.randomUUID())
            .externalCreditorId(externalCreditorId)
            .activityStatus(ActivityStatus.ACTIVE)
            .build();
    entity = creditorRepository.save(entity);
    creditorsByExternalId.put(externalCreditorId, entity);
    return entity;
  }
}
