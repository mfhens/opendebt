package dk.ufst.opendebt.caseservice.limitation;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ufst.opendebt.caseservice.limitation.dto.CreateLimitationObjectionWorkflowRequest;
import dk.ufst.opendebt.caseservice.limitation.dto.LimitationObjectionDecisionRequest;
import dk.ufst.opendebt.caseservice.limitation.dto.LimitationObjectionWorkflowResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/internal/v1/limitation-objections")
@RequiredArgsConstructor
@Tag(
    name = "Limitation objection workflow",
    description = "Internal workflow boundary for petition059")
public class LimitationObjectionWorkflowInternalController {

  private final LimitationObjectionWorkflowService workflowService;

  @PostMapping
  @PreAuthorize("hasRole('SERVICE') or hasRole('ADMIN') or hasRole('CASEWORKER')")
  @Operation(summary = "Create internal limitation objection workflow")
  public ResponseEntity<LimitationObjectionWorkflowResult> createWorkflow(
      @RequestBody CreateLimitationObjectionWorkflowRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.createWorkflow(request));
  }

  @PutMapping("/{indsigelsesId}/decision")
  @PreAuthorize("hasRole('SERVICE') or hasRole('ADMIN') or hasRole('CASEWORKER')")
  @Operation(summary = "Record internal limitation objection decision")
  public ResponseEntity<LimitationObjectionWorkflowResult> recordDecision(
      @PathVariable UUID indsigelsesId, @RequestBody LimitationObjectionDecisionRequest request) {
    return ResponseEntity.ok(workflowService.recordDecision(indsigelsesId, request));
  }
}
