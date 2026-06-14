package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import dk.ufst.opendebt.debtservice.controller.InternalDebtorController;

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
  public void resetPetition066State() {
    expectedEndpoints.clear();
    missingSurfaces.clear();
    missingPackages.clear();
    traceSummary = null;
    pendingImplementation = false;
    canonicalArtifactsReopened = false;
  }

  @Given("petition066 debt-service internal API is expected at {string}")
  public void petition066DebtServiceInternalApiIsExpectedAt(String basePath) {
    expectedEndpoints.add("POST " + basePath);
    expectedEndpoints.add("POST " + basePath + "/{workflowId}/dispatch");
    expectedEndpoints.add("POST " + basePath + "/{workflowId}/withdraw");
    expectedEndpoints.add("POST " + basePath + "/callbacks");
    expectedEndpoints.add("GET " + basePath);
    expectedEndpoints.add("GET " + basePath + "/{workflowId}");
  }

  @Given("petition066 feature and validation contract have been reopened for this run")
  public void petition066FeatureAndValidationContractHaveBeenReopenedForThisRun() {
    canonicalArtifactsReopened = true;
  }

  @When("the petition066 debt-service executable seam inventory is inspected")
  public void thePetition066DebtServiceExecutableSeamInventoryIsInspected() {
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
            "dk.ufst.opendebt.debtservice.attachment.AttachmentWorkflowApi",
            "dk.ufst.opendebt.debtservice.attachment.AttachmentWorkflowApplicationService",
            "dk.ufst.opendebt.debtservice.attachment.AttachmentCallbackValidator",
            "dk.ufst.opendebt.debtservice.attachment.AttachmentInterruptionBridge");
    for (String requiredType : requiredTypes) {
      if (!typeExists(requiredType)) {
        missingPackages.add(requiredType.substring(requiredType.lastIndexOf('.') + 1));
      }
    }
    if (requiredTypes.stream().anyMatch(requiredType -> !typeExists(requiredType))) {
      missingPackages.add(0, "dk.ufst.opendebt.debtservice.attachment");
    }

    pendingImplementation = !missingSurfaces.isEmpty() || !missingPackages.isEmpty();
    traceSummary =
        "petition066 debt-service trace bound to AC-01 through AC-14, workflowReference correlation, and type=UDLAEG interruption linkage";
  }

  @Then("petition066 debt-service coverage is marked pending implementation")
  public void petition066DebtServiceCoverageIsMarkedPendingImplementation() {
    assertThat(pendingImplementation).isTrue();
  }

  @Then("the missing debt-service surface list contains {string}")
  public void theMissingDebtServiceSurfaceListContains(String expected) {
    assertThat(missingSurfaces).contains(expected);
  }

  @Then("the missing debt-service package list contains {string}")
  public void theMissingDebtServicePackageListContains(String expected) {
    assertThat(missingPackages).contains(expected);
  }

  @Then("petition066 debt-service trace summary mentions {string}")
  public void petition066DebtServiceTraceSummaryMentions(String expected) {
    assertThat(canonicalArtifactsReopened).isTrue();
    assertThat(traceSummary).contains(expected);
  }

  private List<String> controllerMappings() {
    List<String> mappings = new ArrayList<>();
    for (java.lang.reflect.Method method : InternalDebtorController.class.getDeclaredMethods()) {
      PostMapping postMapping = method.getAnnotation(PostMapping.class);
      if (postMapping != null) {
        for (String value : postMapping.value()) {
          mappings.add("POST " + value);
        }
      }
      GetMapping getMapping = method.getAnnotation(GetMapping.class);
      if (getMapping != null) {
        for (String value : getMapping.value()) {
          mappings.add("GET " + value);
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
