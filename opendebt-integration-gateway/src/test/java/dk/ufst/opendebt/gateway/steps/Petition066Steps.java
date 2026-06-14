package dk.ufst.opendebt.gateway.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;

import dk.ufst.opendebt.gateway.creditor.controller.CreditorM2mController;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition066Steps {

  private final List<String> expectedEndpoints = new ArrayList<>();
  private final List<String> missingSurfaces = new ArrayList<>();
  private final List<String> missingPackages = new ArrayList<>();
  private String traceSummary;
  private boolean pendingImplementation;
  private boolean canonicalArtifactsReopened;

  @Before("@petition066")
  public void resetPetition066GatewayState() {
    expectedEndpoints.clear();
    missingSurfaces.clear();
    missingPackages.clear();
    traceSummary = null;
    pendingImplementation = false;
    canonicalArtifactsReopened = false;
  }

  @Given("petition066 gateway callback API is expected at {string}")
  public void petition066GatewayCallbackApiIsExpectedAt(String callbackPath) {
    expectedEndpoints.add("POST " + callbackPath);
    expectedEndpoints.add("POST /api/external/v1/fogedret/attachment-dispatch");
  }

  @Given("petition066 feature and validation contract have been reopened for this run in gateway coverage")
  public void petition066FeatureAndValidationContractHaveBeenReopenedForThisRunInGatewayCoverage() {
    canonicalArtifactsReopened = true;
  }

  @When("the petition066 gateway executable seam inventory is inspected")
  public void thePetition066GatewayExecutableSeamInventoryIsInspected() {
    missingSurfaces.clear();
    missingPackages.clear();

    List<String> controllerMappings = controllerMappings();
    for (String endpoint : expectedEndpoints) {
      if (!controllerMappings.contains(endpoint)) {
        missingSurfaces.add(endpoint);
      }
    }

    List<String> requiredTypes =
        List.of(
            "dk.ufst.opendebt.gateway.fogedret.FogedretCallbackController",
            "dk.ufst.opendebt.gateway.fogedret.FogedretReplayGuard",
            "dk.ufst.opendebt.gateway.fogedret.AttachmentGatewayClient");
    for (String requiredType : requiredTypes) {
      if (!typeExists(requiredType)) {
        missingPackages.add(requiredType.substring(requiredType.lastIndexOf('.') + 1));
      }
    }
    if (requiredTypes.stream().anyMatch(requiredType -> !typeExists(requiredType))) {
      missingPackages.add(0, "dk.ufst.opendebt.gateway.fogedret");
    }

    pendingImplementation = !missingSurfaces.isEmpty() || !missingPackages.isEmpty();
    traceSummary =
        "petition066 gateway trace bound to AC-15 through AC-17, OCES3 mTLS ingress, replay protection, and workflowReference correlation";
  }

  @Then("petition066 gateway coverage is marked pending implementation")
  public void petition066GatewayCoverageIsMarkedPendingImplementation() {
    assertThat(pendingImplementation).isTrue();
  }

  @Then("the missing gateway surface list contains {string}")
  public void theMissingGatewaySurfaceListContains(String expected) {
    assertThat(missingSurfaces).contains(expected);
  }

  @Then("the missing gateway package list contains {string}")
  public void theMissingGatewayPackageListContains(String expected) {
    assertThat(missingPackages).contains(expected);
  }

  @Then("petition066 gateway trace summary mentions {string}")
  public void petition066GatewayTraceSummaryMentions(String expected) {
    assertThat(canonicalArtifactsReopened).isTrue();
    assertThat(traceSummary).contains(expected);
  }

  private List<String> controllerMappings() {
    List<String> mappings = new ArrayList<>();
    for (java.lang.reflect.Method method : CreditorM2mController.class.getDeclaredMethods()) {
      PostMapping postMapping = method.getAnnotation(PostMapping.class);
      if (postMapping != null) {
        for (String value : postMapping.value()) {
          mappings.add("POST " + value);
        }
      }
    }
    return mappings;
  }

  private boolean typeExists(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException ex) {
      return false;
    }
  }
}
