package dk.ufst.opendebt.gateway.creditor.controller;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.gateway.creditor.dto.*;
import dk.ufst.opendebt.gateway.creditor.service.CreditorIngressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/creditor-m2m")
@RequiredArgsConstructor
@Tag(name = "Creditor M2M", description = "Machine-to-machine creditor ingress via DUPLA")
public class CreditorM2mController {

  private final CreditorIngressService creditorIngressService;

  @PostMapping("/claims/submit")
  @PreAuthorize("hasRole('SYSTEM') or hasRole('ADMIN')")
  @Operation(
      summary = "Submit a claim through the M2M channel",
      description =
          "External creditor systems submit claims through DUPLA. The gateway resolves "
              + "the acting creditor, validates access, and routes to debt-service.")
  public ResponseEntity<GatewayClaimResponse> submitClaim(
      @RequestHeader("X-Creditor-Identity") String presentedIdentity,
      @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
      @Valid @RequestBody ClaimSubmissionRequest request) {

    String effectiveCorrelationId =
        correlationId != null ? correlationId : UUID.randomUUID().toString();

    log.info(
        "M2M claim submission: identity={} correlationId={}",
        presentedIdentity,
        effectiveCorrelationId);

    GatewayClaimResponse response =
        creditorIngressService.submitClaim(presentedIdentity, request, effectiveCorrelationId);

    HttpStatus status =
        response.getOutcome() == GatewayClaimResponse.Outcome.REJECTED
            ? HttpStatus.UNPROCESSABLE_ENTITY
            : HttpStatus.CREATED;

    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(OpenDebtException.class)
  public ResponseEntity<GatewayErrorResponse> handleOpenDebtException(OpenDebtException e) {
    log.warn("M2M request failed: code={} message={}", e.getErrorCode(), e.getMessage());

    HttpStatus status =
        "M2M_ACCESS_DENIED".equals(e.getErrorCode()) || "ACCESS_DENIED".equals(e.getErrorCode())
            ? HttpStatus.FORBIDDEN
            : HttpStatus.BAD_GATEWAY;

    GatewayErrorResponse error =
        GatewayErrorResponse.builder()
            .errorCode(e.getErrorCode())
            .message(e.getMessage())
            .timestamp(Instant.now())
            .build();

    return ResponseEntity.status(status).body(error);
  }
}
