package dk.ufst.opendebt.debtservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ufst.opendebt.debtservice.attachment.AttachmentWorkflowApi;
import dk.ufst.opendebt.debtservice.attachment.AttachmentWorkflowCallbackRequest;
import dk.ufst.opendebt.debtservice.attachment.AttachmentWorkflowDto;
import dk.ufst.opendebt.debtservice.attachment.CreateAttachmentWorkflowRequest;
import dk.ufst.opendebt.debtservice.attachment.WithdrawAttachmentWorkflowRequest;
import dk.ufst.opendebt.debtservice.dto.ActiveFordringResponseDto;
import dk.ufst.opendebt.debtservice.service.ActiveFordringService;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Internal service-to-service APIs for debtor data.
 *
 * <p>These endpoints are NOT part of the public OpenAPI spec (annotated {@link Hidden}) and are NOT
 * exposed through the public API gateway. They are consumed directly by other internal services
 * (e.g. payment-service) using their SERVICE-scoped JWT bearer token.
 *
 * <p>Security: all methods require the {@code SERVICE} role, which is granted to internal service
 * accounts via the OAuth2 token exchange flow. This follows the existing pattern used in {@link
 * DebtController} for {@code /evaluate-state} and interest-recalculation endpoints.
 *
 * <p>GDPR: path parameter {@code debtorId} is a UUID ({@code person_id}), never a CPR number.
 * Responses contain no CPR, name, or address.
 */
@RestController
@RequestMapping("/internal/debtors")
@RequiredArgsConstructor
@Hidden // exclude from public Swagger UI / OpenAPI spec
@Tag(name = "Internal Debtor API", description = "Service-to-service APIs — not public")
public class InternalDebtorController {

  private final ActiveFordringService activeFordringService;
  private final AttachmentWorkflowApi attachmentWorkflowApi;

  /**
   * Returns all active fordringer for the given debtor, ordered for dækningsrækkefølge (P057).
   *
   * <p>"Active" means: remaining balance > 0 and status not in {PAID, WRITTEN_OFF, CANCELLED}.
   *
   * <p>Called by payment-service during payment application (P057) to replace the interim {@code
   * DaekningFordringRepository} cache (TB-040 / DEP-1).
   *
   * @param debtorId person-registry UUID — never CPR
   * @return 200 with ordered list; empty array when no active fordringer exist
   */
  @GetMapping("/{debtorId}/fordringer/active")
  @PreAuthorize("hasRole('SERVICE')")
  @Operation(
      summary = "Get active fordringer for a debtor (internal)",
      description =
          "Returns all active fordringer (remaining balance > 0, non-terminal status) ordered by "
              + "sekvensNummer ASC then applicationTimestamp ASC. Consumed by payment-service "
              + "for dækningsrækkefølge calculation (P057 / TB-040).")
  public ResponseEntity<List<ActiveFordringResponseDto>> getActiveFordringer(
      @Parameter(description = "Debtor person-registry UUID (never CPR)") @PathVariable
          UUID debtorId) {
    return ResponseEntity.ok(activeFordringService.getActiveFordringer(debtorId));
  }

  @PostMapping("/{debtorId}/attachment-workflows")
  @PreAuthorize("hasRole('SERVICE')")
  public ResponseEntity<AttachmentWorkflowDto> createAttachmentWorkflow(
      @PathVariable UUID debtorId, @org.springframework.web.bind.annotation.RequestBody CreateAttachmentWorkflowRequest request) {
    return ResponseEntity.ok(attachmentWorkflowApi.createWorkflow(debtorId, request));
  }

  @PostMapping("/{debtorId}/attachment-workflows/{workflowId}/dispatch")
  @PreAuthorize("hasRole('SERVICE')")
  public ResponseEntity<AttachmentWorkflowDto> dispatchAttachmentWorkflow(
      @PathVariable UUID debtorId, @PathVariable UUID workflowId) {
    return ResponseEntity.ok(attachmentWorkflowApi.dispatchWorkflow(debtorId, workflowId));
  }

  @PostMapping("/{debtorId}/attachment-workflows/{workflowId}/withdraw")
  @PreAuthorize("hasRole('SERVICE')")
  public ResponseEntity<AttachmentWorkflowDto> withdrawAttachmentWorkflow(
      @PathVariable UUID debtorId,
      @PathVariable UUID workflowId,
      @org.springframework.web.bind.annotation.RequestBody WithdrawAttachmentWorkflowRequest request) {
    return ResponseEntity.ok(attachmentWorkflowApi.withdrawWorkflow(debtorId, workflowId, request));
  }

  @PostMapping("/{debtorId}/attachment-workflows/callbacks")
  @PreAuthorize("hasRole('SERVICE')")
  public ResponseEntity<AttachmentWorkflowDto> processAttachmentWorkflowCallback(
      @PathVariable UUID debtorId,
      @org.springframework.web.bind.annotation.RequestBody AttachmentWorkflowCallbackRequest request) {
    return ResponseEntity.ok(attachmentWorkflowApi.processCallback(debtorId, request));
  }

  @GetMapping("/{debtorId}/attachment-workflows")
  @PreAuthorize("hasRole('SERVICE')")
  public ResponseEntity<List<AttachmentWorkflowDto>> getAttachmentWorkflows(@PathVariable UUID debtorId) {
    return ResponseEntity.ok(attachmentWorkflowApi.getWorkflows(debtorId));
  }

  @GetMapping("/{debtorId}/attachment-workflows/{workflowId}")
  @PreAuthorize("hasRole('SERVICE')")
  public ResponseEntity<AttachmentWorkflowDto> getAttachmentWorkflow(
      @PathVariable UUID debtorId, @PathVariable UUID workflowId) {
    return ResponseEntity.ok(attachmentWorkflowApi.getWorkflow(debtorId, workflowId));
  }
}
