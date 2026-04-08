package dk.ufst.opendebt.debtservice.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentRequestDto;
import dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentResponseDto;
import dk.ufst.opendebt.debtservice.exception.CreditorValidationException;
import dk.ufst.opendebt.debtservice.service.ClaimAdjustmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for creditor-portal claim adjustments (write-ups and write-downs).
 *
 * <p>Endpoint: {@code POST /api/v1/debts/{id}/adjustments}
 *
 * <p>Auth: CREDITOR or ADMIN role required.
 *
 * <p>Success: HTTP 201 with {@link ClaimAdjustmentResponseDto} body.
 *
 * <p>Validation failure: HTTP 422 with RFC 7807 ProblemDetail body.
 *
 * <p>Spec reference: SPEC-P053 §9.2
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/debts/{id}/adjustments")
@RequiredArgsConstructor
@Tag(name = "Claim Adjustments", description = "Creditor-portal write-up and write-down endpoints")
public class ClaimAdjustmentController {

  private static final URI PROBLEM_TYPE_URI =
      URI.create("https://opendebt.ufst.dk/problems/validation-failure");

  private final ClaimAdjustmentService claimAdjustmentService;

  /**
   * Submits a claim adjustment (write-up or write-down) for the specified claim.
   *
   * <p>All FR-9 validation rules are enforced by {@link
   * dk.ufst.opendebt.debtservice.service.impl.ClaimAdjustmentServiceImpl}.
   *
   * @param id the UUID of the claim to adjust
   * @param request the adjustment request body
   * @return HTTP 201 with the adjustment response
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('CREDITOR') or hasRole('ADMIN')")
  @Operation(
      summary = "Submit claim adjustment",
      description =
          "Submit a write-up or write-down adjustment for a claim. Enforces all"
              + " G.A.1.4.3/G.A.1.4.4/Gæld.bekendtg. § 7 validation rules independently of the"
              + " portal (FR-9 / SPEC-P053 §9.2).")
  public ResponseEntity<ClaimAdjustmentResponseDto> submitAdjustment(
      @PathVariable UUID id, @RequestBody ClaimAdjustmentRequestDto request) {

    log.info("Received adjustment request for claim {} type={}", id, request.getAdjustmentType());

    ClaimAdjustmentResponseDto response = claimAdjustmentService.processAdjustment(id, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Maps {@link HttpMessageNotReadableException} (e.g. invalid enum value in JSON body) to HTTP 422
   * Unprocessable Entity with RFC 7807 ProblemDetail body (SPEC-P053 §9.5 / B5).
   *
   * <p>This prevents Jackson deserialization errors for bad enum values (e.g. {@code
   * "writeDownReasonCode": "GARBAGE"}) from leaking as HTTP 400 instead of 422.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex) {
    log.warn("Claim adjustment request deserialization failure: {}", ex.getMessage());

    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    problem.setType(PROBLEM_TYPE_URI);
    problem.setTitle("Unprocessable Entity");
    problem.setDetail(
        "Request body could not be parsed. Check enum values and field formats: "
            + ex.getMostSpecificCause().getMessage());

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
  }

  /**
   * Maps {@link CreditorValidationException} to HTTP 422 Unprocessable Entity with RFC 7807
   * ProblemDetail body (SPEC-P053 §9.5).
   *
   * <p>Response shape:
   *
   * <pre>{@code
   * {
   *   "type": "https://opendebt.ufst.dk/problems/validation-failure",
   *   "title": "Unprocessable Entity",
   *   "status": 422,
   *   "detail": "<rule-specific message>"
   * }
   * }</pre>
   */
  @ExceptionHandler(CreditorValidationException.class)
  public ResponseEntity<ProblemDetail> handleCreditorValidationException(
      CreditorValidationException ex) {
    log.warn("Claim adjustment validation failure: {} ({})", ex.getMessage(), ex.getErrorCode());

    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    problem.setType(PROBLEM_TYPE_URI);
    problem.setTitle("Unprocessable Entity");
    problem.setDetail(ex.getMessage());

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
  }
}
