package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * BDD step definitions for petition 053 — debt-service backend enforcement (FR-9) and audit log
 * (AC-16).
 *
 * <p>Covered requirements:
 *
 * <ul>
 *   <li>FR-9 / FR-1: WriteDownReasonCode required and validated at API boundary (HTTP 422)
 *   <li>FR-9 / FR-2: OPSKRIVNING_REGULERING on RENTE claim → HTTP 422
 *   <li>FR-9 / FR-3: Høring timing rule applied server-side
 *   <li>FR-9 / FR-4: Retroactive nedskrivning log marker (WARN level)
 *   <li>FR-9 / FR-7: DINDB/OMPL/AFSK write-up reason codes → HTTP 422
 *   <li>AC-16: All adjustment operations (success and failure) logged to CLS
 * </ul>
 *
 * <p><strong>Failing strategy:</strong> All When and Then steps throw {@link PendingException}
 * because the target classes do not yet exist:
 *
 * <ul>
 *   <li>{@code dk.ufst.opendebt.debtservice.controller.ClaimAdjustmentController} (SPEC-P053 §9.2)
 *   <li>{@code dk.ufst.opendebt.debtservice.service.ClaimAdjustmentService} (SPEC-P053 §9.1)
 *   <li>{@code dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentRequestDto} (SPEC-P053 §9.0)
 *   <li>{@code dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentResponseDto} (SPEC-P053 §6.1)
 *   <li>{@code dk.ufst.opendebt.debtservice.dto.WriteDownReasonCode} (SPEC-P053 §1.3)
 * </ul>
 *
 * <p>Given steps use existing {@link DebtRepository} to seed the H2 test database so that the claim
 * entity is available when the endpoint eventually exists.
 *
 * <p>Spec reference: SPEC-P053, design/specs-p053-opskrivning-nedskrivning.md §9
 */
public class Petition053Steps {

  // ── Spring-managed collaborators ────────────────────────────────────────────

  @Autowired private DebtRepository debtRepository;

  // ── Per-scenario state ──────────────────────────────────────────────────────

  /**
   * Maps external claim references (e.g. "FDR-90070") to their persisted {@link DebtEntity} UUIDs.
   * Populated by Given steps; consumed by When steps to resolve the path variable.
   */
  private final Map<String, UUID> claimIndex = new HashMap<>();

  /** Pending claim category to apply when the entity is created (Given steps, FR-2). */
  private String pendingClaimCategory;

  @Before("@petition053")
  public void setUp() {
    debtRepository.deleteAll();
    claimIndex.clear();
    pendingClaimCategory = null;
  }

  // ── Given steps ─────────────────────────────────────────────────────────────

  /**
   * Context marker — actual HTTP call is made in the When step. No infrastructure setup required;
   * the Spring application context is already running.
   */
  @Given("a direct API call is made to the debt-service adjustment endpoint")
  public void directApiCallToAdjustmentEndpoint() {
    // POST /api/v1/debts/{id}/adjustments — ClaimAdjustmentController (SPEC-P053 §9.2).
    // Endpoint does not exist yet; the When step will throw PendingException.
  }

  /**
   * Creates a {@link DebtEntity} with lifecycle state {@code OVERDRAGET} (under inddrivelse) and
   * registers it in {@link #claimIndex} under the given external reference.
   */
  @Given("a claim {string} is under inddrivelse")
  public void claimIsUnderInddrivelse(String claimRef) {
    UUID id = seedClaim(claimRef, ClaimLifecycleState.OVERDRAGET);
    claimIndex.put(claimRef, id);
  }

  /**
   * FR-9 / FR-2: Creates a claim and records that it carries the given claim category. The category
   * is stored for use in the When step assertion message; the DebtEntity seed uses the existing
   * builder pattern (category enforcement is service-layer).
   */
  @Given("claim {string} has claim category {string}")
  public void claimHasClaimCategory(String claimRef, String category) {
    pendingClaimCategory = category;
    if (!claimIndex.containsKey(claimRef)) {
      UUID id = seedClaim(claimRef, ClaimLifecycleState.OVERDRAGET);
      claimIndex.put(claimRef, id);
    }
  }

  /**
   * FR-9 / FR-3: Creates a claim with the given {@link ClaimLifecycleState} (e.g. {@code HOERING}).
   */
  @Given("claim {string} has lifecycleState {string}")
  public void claimHasLifecycleState(String claimRef, String lifecycleStateName) {
    ClaimLifecycleState state = ClaimLifecycleState.valueOf(lifecycleStateName);
    assertThat(ClaimLifecycleState.values())
        .as("Lifecycle state '%s' must be a known ClaimLifecycleState value.", lifecycleStateName)
        .contains(state);
    if (!claimIndex.containsKey(claimRef)) {
      UUID id = seedClaim(claimRef, state);
      claimIndex.put(claimRef, id);
    }
  }

  // ── When steps — all pending (ClaimAdjustmentController does not exist) ─────

  /**
   * FR-9 / FR-1 / AC-11 (SPEC-P053 §9.3): POST an adjustment request with direction WRITE_DOWN and
   * a null {@code writeDownReasonCode}. Expected: HTTP 422.
   *
   * <p><strong>Pending:</strong> {@code ClaimAdjustmentController} (the new REST controller at
   * {@code POST /api/v1/debts/{id}/adjustments}) does not exist yet. {@code
   * ClaimAdjustmentRequestDto} (debt-service DTO, SPEC-P053 §9.0) does not exist yet.
   */
  @When("a WriteDownDto is submitted for claim {string} without a reasonCode")
  public void writeDownDtoSubmittedWithoutReasonCode(String claimRef) {
    throw new PendingException(
        "Not implemented: POST /api/v1/debts/{id}/adjustments with WRITE_DOWN direction "
            + "and writeDownReasonCode=null (FR-9 / FR-1 / AC-11, SPEC-P053 §9.2, §9.3). "
            + "Create: ClaimAdjustmentController, ClaimAdjustmentRequestDto (debt-service), "
            + "ClaimAdjustmentServiceImpl.processAdjustment() with WriteDownReasonCode null-check.");
  }

  /**
   * FR-9 / FR-1 / AC-11 (SPEC-P053 §9.3): POST with an explicit reason code value. Used for both
   * valid (NED_GRUNDLAG_AENDRET → 201) and invalid (UNKNOWN_CODE → 422) cases.
   *
   * <p><strong>Pending:</strong> {@code ClaimAdjustmentController} and {@code WriteDownReasonCode}
   * enum (debt-service) do not exist yet.
   */
  @When("a WriteDownDto is submitted for claim {string} with reasonCode {string}")
  public void writeDownDtoSubmittedWithReasonCode(String claimRef, String reasonCode) {
    throw new PendingException(
        "Not implemented: POST /api/v1/debts/{id}/adjustments with writeDownReasonCode='"
            + reasonCode
            + "' (FR-9 / FR-1, SPEC-P053 §9.2, §9.3). "
            + "Create: dk.ufst.opendebt.debtservice.dto.WriteDownReasonCode enum "
            + "(NED_INDBETALING, NED_FEJL_OVERSENDELSE, NED_GRUNDLAG_AENDRET) "
            + "and ClaimAdjustmentController endpoint.");
  }

  /**
   * FR-9 / FR-4: POST with full DataTable fields including {@code virkningsdato} in the past.
   * Expected: HTTP 201 + retroactive WARN log marker.
   *
   * <p><strong>Pending:</strong> {@code ClaimAdjustmentController} and {@code
   * ClaimAdjustmentServiceImpl} retroactive log path not yet implemented.
   */
  @When("a WriteDownDto is submitted for claim {string} with:")
  public void writeDownDtoSubmittedWithDataTable(String claimRef, DataTable dataTable) {
    throw new PendingException(
        "Not implemented: POST /api/v1/debts/{id}/adjustments with full WriteDownDto "
            + "(FR-9 / FR-4, SPEC-P053 §9.2, §4.3). "
            + "Create ClaimAdjustmentRequestDto (debt-service) with effectiveDate field "
            + "and ClaimAdjustmentServiceImpl retroactive WARN log: "
            + "log.warn(\"RETROACTIVE_NEDSKRIVNING claimId={} virkningsdato={}\", ...).");
  }

  /**
   * FR-9 / FR-2 (SPEC-P053 §9.3): POST a write-up of type OPSKRIVNING_REGULERING on a
   * RENTE-category claim. Expected: HTTP 422 with ProblemDetail.
   *
   * <p><strong>Pending:</strong> {@code ClaimAdjustmentServiceImpl} RENTE category check not yet
   * implemented.
   */
  @When("a write-up of type {string} is submitted for claim {string}")
  public void writeUpOfTypeSubmittedForClaim(String adjustmentType, String claimRef) {
    throw new PendingException(
        "Not implemented: POST /api/v1/debts/{id}/adjustments WRITE_UP with adjustmentType='"
            + adjustmentType
            + "' on claim category '"
            + pendingClaimCategory
            + "' (FR-9 / FR-2, SPEC-P053 §9.3). "
            + "Add RENTE + OPSKRIVNING_REGULERING rejection rule to "
            + "ClaimAdjustmentServiceImpl.processAdjustment(): "
            + "\"RENTE claims must use a rentefordring, not an opskrivningsfordring (G.A.1.4.3)\".");
  }

  /**
   * FR-9 / FR-7 (SPEC-P053 §9.3, §7.4): POST a write-up carrying a RIM-internal reason code.
   * Expected: HTTP 422. Denylist: {@code Set.of("DINDB", "OMPL", "AFSK")}.
   *
   * <p><strong>Pending:</strong> {@code ClaimAdjustmentServiceImpl} denylist check ({@code
   * RIM_INTERNAL_CODES.contains(request.getWriteUpReasonCode())}) not yet implemented.
   */
  @When("a write-up is submitted for claim {string} with reasonCode {string}")
  public void writeUpSubmittedWithReasonCode(String claimRef, String reasonCode) {
    throw new PendingException(
        "Not implemented: POST /api/v1/debts/{id}/adjustments WRITE_UP with writeUpReasonCode='"
            + reasonCode
            + "' (FR-9 / FR-7, SPEC-P053 §9.3, §7.4). "
            + "Add denylist check to ClaimAdjustmentServiceImpl: "
            + "private static final Set<String> RIM_INTERNAL_CODES = Set.of(\"DINDB\",\"OMPL\",\"AFSK\"); "
            + "throw CreditorValidationException when match found.");
  }

  /**
   * FR-9 / FR-3 (SPEC-P053 §9.3): POST a write-up for a claim in HOERING state. Expected: receipt
   * timestamp set to høring resolution time (not portal submission time).
   *
   * <p><strong>Pending:</strong> {@code ClaimAdjustmentServiceImpl} høring timing rule not yet
   * implemented.
   */
  @When("an opskrivning is submitted for claim {string}")
  public void opskrivningSubmittedForClaim(String claimRef) {
    throw new PendingException(
        "Not implemented: POST /api/v1/debts/{id}/adjustments WRITE_UP for claim in HOERING "
            + "state (FR-9 / FR-3, SPEC-P053 §9.3). "
            + "Implement høring timing rule in ClaimAdjustmentServiceImpl: "
            + "set opskrivningsfordring receipt timestamp = høring resolution time, "
            + "not LocalDateTime.now() at portal submission.");
  }

  /**
   * AC-16 (SPEC-P053 §9.4): Submit a valid nedskrivning via direct API call. Expected: HTTP 201 +
   * CLS audit entry with outcome SUCCESS.
   *
   * <p><strong>Pending:</strong> {@code ClaimAdjustmentController} and CLS logging in {@code
   * ClaimAdjustmentServiceImpl.processAdjustment()} not yet implemented.
   */
  @When(
      "a valid nedskrivning is submitted via a direct API call for claim {string} with reasonCode {string}")
  public void validNedskrivningSubmittedViaDirectApiCall(String claimRef, String reasonCode) {
    throw new PendingException(
        "Not implemented: POST /api/v1/debts/{id}/adjustments with valid reasonCode='"
            + reasonCode
            + "' and CLS SUCCESS audit logging (AC-16, SPEC-P053 §9.4). "
            + "Implement ClaimAdjustmentController and clsAuditService.record(..., SUCCESS, ...) "
            + "call in ClaimAdjustmentServiceImpl.processAdjustment(). "
            + "Note: TestConfig mocks ClsAuditClient(isEnabled()=false); "
            + "update TestConfig or use a Mockito.verify() on the captured call.");
  }

  /**
   * AC-16 (SPEC-P053 §9.4): Submit an invalid WriteDownDto without reasonCode via direct API.
   * Expected: HTTP 422 + CLS audit entry with outcome FAILURE.
   *
   * <p><strong>Pending:</strong> Same reasons as above — endpoint does not exist.
   */
  @When("a WriteDownDto is submitted via a direct API call for claim {string} without a reasonCode")
  public void writeDownDtoSubmittedViaDirectApiCallWithoutReasonCode(String claimRef) {
    throw new PendingException(
        "Not implemented: POST /api/v1/debts/{id}/adjustments without reasonCode "
            + "and CLS FAILURE audit logging (AC-16, SPEC-P053 §9.4). "
            + "clsAuditService.record(..., FAILURE, ...) must be called in the exception handler "
            + "path of ClaimAdjustmentServiceImpl before throwing CreditorValidationException.");
  }

  // ── Then steps — all pending (no HTTP response to assert on) ────────────────

  /**
   * FR-9 / AC-16: Assert the expected HTTP status code.
   *
   * <p><strong>Pending:</strong> No HTTP response exists because the When step threw {@link
   * PendingException}. Once the endpoint is implemented, use {@code
   * MockMvc.perform(...).andExpect(status().is(expectedStatus))}.
   */
  @Then("debt-service returns HTTP status {int}")
  public void debtServiceReturnsHttpStatus(int expectedStatus) {
    throw new PendingException(
        "Not implemented: assert HTTP "
            + expectedStatus
            + " from POST /api/v1/debts/{id}/adjustments (FR-9 / AC-16, SPEC-P053 §9.2, §9.5). "
            + "Endpoint does not exist — current response would be HTTP 404. "
            + "Implement ClaimAdjustmentController and inject MockMvc into this step class "
            + "to assert: mockMvc.perform(post(...)).andExpect(status().is("
            + expectedStatus
            + ")).");
  }

  /**
   * FR-9 (SPEC-P053 §9.5): Assert RFC 7807 ProblemDetail body for HTTP 422 responses.
   *
   * <p>Expected body shape:
   *
   * <pre>{@code
   * {
   *   "type": "https://opendebt.ufst.dk/problems/validation-failure",
   *   "title": "Unprocessable Entity",
   *   "status": 422,
   *   "detail": "<rule-specific detail message>"
   * }
   * }</pre>
   */
  @Then("the response body contains a problem-detail describing the validation failure")
  public void responseBodyContainsProblemDetail() {
    throw new PendingException(
        "Not implemented: assert RFC 7807 ProblemDetail body for HTTP 422 (SPEC-P053 §9.5). "
            + "Requires ClaimAdjustmentController global exception handler for "
            + "CreditorValidationException → ProblemDetail with type "
            + "'https://opendebt.ufst.dk/problems/validation-failure'. "
            + "Use MockMvc.andExpect(jsonPath(\"$.status\").value(422)) "
            + "and jsonPath(\"$.detail\").isNotEmpty().");
  }

  /**
   * AC-16 (SPEC-P053 §9.4): Assert that a CLS audit entry was created for the given claim with the
   * specified outcome (SUCCESS or FAILURE).
   *
   * <p><strong>Pending:</strong> {@code ClaimAdjustmentServiceImpl.processAdjustment()} CLS logging
   * not yet implemented. {@code TestConfig} mocks {@code ClsAuditClient} with {@code
   * isEnabled()=false} — the test configuration must be updated (or a dedicated test-enabled mock
   * must be wired) before this assertion can be verified.
   */
  @Then("an audit log entry is created in CLS for claim {string} with outcome {string}")
  public void auditLogEntryCreatedInClsWithOutcome(String claimRef, String outcome) {
    throw new PendingException(
        "Not implemented: assert CLS audit entry with outcome='"
            + outcome
            + "' for claim '"
            + claimRef
            + "' (AC-16, SPEC-P053 §9.4). "
            + "clsAuditService.record(claimId, adjustmentType, reasonCode, "
            + outcome
            + ", creditorId, now()) call not yet in ClaimAdjustmentServiceImpl. "
            + "TestConfig sets ClsAuditClient.isEnabled()=false — update TestConfig "
            + "to capture audit calls via Mockito.verify(clsAuditClient).record(...).");
  }

  /**
   * FR-9 / FR-3 (SPEC-P053 §9.3): Assert that the opskrivningsfordring's receipt timestamp equals
   * the høring resolution time (not the portal submission time).
   *
   * <p><strong>Pending:</strong> Høring timing rule in {@code ClaimAdjustmentServiceImpl} not yet
   * implemented.
   */
  @Then(
      "debt-service records the opskrivningsfordring receipt timestamp as the høring resolution time")
  public void debtServiceRecordsOpskrivningsfordringTimestampAsHoeringResolution() {
    throw new PendingException(
        "Not implemented: assert høring timing rule — opskrivningsfordring receipt timestamp "
            + "== høring resolution time (FR-9 / FR-3, SPEC-P053 §9.3). "
            + "ClaimAdjustmentServiceImpl must set receipt timestamp = HoeringEntity.resolvedAt "
            + "when claim.lifecycleState == HOERING. Endpoint does not exist yet.");
  }

  /**
   * FR-9 / FR-3 (SPEC-P053 §9.3): Assert that the portal submission timestamp is NOT used as the
   * opskrivningsfordring receipt time for høring claims.
   *
   * <p><strong>Pending:</strong> Same as above — endpoint and høring rule not yet implemented.
   */
  @Then("debt-service does not use the portal submission timestamp as the receipt time")
  public void debtServiceDoesNotUsePortalSubmissionTimestampAsReceiptTime() {
    throw new PendingException(
        "Not implemented: assert portal submission timestamp is NOT the receipt time for "
            + "høring claims (FR-9 / FR-3, SPEC-P053 §9.3). "
            + "Verified in conjunction with the høring resolution timestamp assertion above.");
  }

  /**
   * FR-4 / FR-9 (SPEC-P053 §4.3): Assert that a structured WARN log marker is emitted when {@code
   * effectiveDate < LocalDate.now()}.
   *
   * <p>Expected log entry (SPEC-P053 §4.3):
   *
   * <pre>
   * log.warn("RETROACTIVE_NEDSKRIVNING claimId={} virkningsdato={}", claimId, effectiveDate);
   * </pre>
   *
   * <p><strong>Pending:</strong> Retroactive log path in {@code ClaimAdjustmentServiceImpl} not yet
   * implemented. Capturing log output requires a test Logback appender (e.g. {@code
   * ch.qos.logback.core.read.ListAppender}) registered in the test context.
   */
  @Then("a retroactive nedskrivning log marker is emitted for claim {string}")
  public void retroactiveNedskrivningLogMarkerEmitted(String claimRef) {
    throw new PendingException(
        "Not implemented: assert WARN log marker 'RETROACTIVE_NEDSKRIVNING' emitted for claim '"
            + claimRef
            + "' (FR-4 / FR-9, SPEC-P053 §4.3). "
            + "ClaimAdjustmentServiceImpl retroactive log branch not yet implemented. "
            + "After implementation: register a ListAppender<ILoggingEvent> in the test context "
            + "and assert appender.list contains event with message containing "
            + "'RETROACTIVE_NEDSKRIVNING' and formattedMessage containing claimId.");
  }

  // ── Helper ──────────────────────────────────────────────────────────────────

  /**
   * Seeds a {@link DebtEntity} in the H2 test database and returns its generated UUID. Uses a
   * {@code receivedAt} value in the past to support FR-6 cross-system comparison ({@code
   * request.getEffectiveDate().isBefore(debt.getReceivedAt().toLocalDate())}).
   */
  private UUID seedClaim(String externalReference, ClaimLifecycleState lifecycleState) {
    DebtEntity debt =
        DebtEntity.builder()
            .debtorPersonId(UUID.randomUUID())
            .creditorOrgId(UUID.randomUUID())
            .debtTypeCode("600")
            .principalAmount(BigDecimal.valueOf(10_000))
            .outstandingBalance(BigDecimal.valueOf(10_000))
            .dueDate(LocalDate.now().minusMonths(2))
            .externalReference(externalReference)
            .status(DebtEntity.DebtStatus.ACTIVE)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .lifecycleState(lifecycleState)
            // receivedAt set to 24 months ago so that virkningsdato scenarios with dates
            // like "2022-12-01" or "60 days ago" are reliably before or after PSRM date
            // depending on the scenario. Override per-scenario if needed.
            .receivedAt(LocalDateTime.now().minusMonths(24))
            .build();
    return debtRepository.save(debt).getId();
  }
}
