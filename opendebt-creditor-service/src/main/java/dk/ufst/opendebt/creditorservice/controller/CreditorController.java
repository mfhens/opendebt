package dk.ufst.opendebt.creditorservice.controller;

import java.util.*;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.creditorservice.dto.*;
import dk.ufst.opendebt.creditorservice.service.CreditorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/creditors")
@RequiredArgsConstructor
@Tag(name = "Creditors", description = "Creditor master-data operations")
public class CreditorController {

  private final CreditorService creditorService;

  @GetMapping("/{creditorOrgId}")
  @PreAuthorize("hasRole('SERVICE') or hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Get creditor by organization reference",
      description =
          "Resolve creditor master data by technical UUID reference to person-registry organization")
  public ResponseEntity<CreditorDto> getByCreditorOrgId(@PathVariable UUID creditorOrgId) {
    CreditorDto dto = creditorService.getByCreditorOrgId(creditorOrgId);
    return ResponseEntity.ok(dto);
  }

  @GetMapping("/by-external-id/{externalCreditorId}")
  @PreAuthorize("hasRole('SERVICE') or hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Get creditor by legacy external identifier",
      description = "Resolve creditor by unique legacy/business identifier")
  public ResponseEntity<CreditorDto> getByExternalCreditorId(
      @PathVariable String externalCreditorId) {
    CreditorDto dto = creditorService.getByExternalCreditorId(externalCreditorId);
    return ResponseEntity.ok(dto);
  }

  @PostMapping("/{creditorOrgId}/validate-action")
  @PreAuthorize("hasRole('SERVICE') or hasRole('CASEWORKER')")
  @Operation(
      summary = "Validate creditor status and permission for an action",
      description =
          "Verify creditor can perform requested action based on permissions and activity status")
  public ResponseEntity<ValidateActionResponse> validateAction(
      @PathVariable UUID creditorOrgId, @Valid @RequestBody ValidateActionRequest request) {
    ValidateActionResponse response = creditorService.validateAction(creditorOrgId, request);
    return ResponseEntity.ok(response);
  }
}
