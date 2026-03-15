package dk.ufst.opendebt.creditorservice.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.creditorservice.dto.*;
import dk.ufst.opendebt.creditorservice.service.ChannelBindingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/creditors/access")
@RequiredArgsConstructor
@Tag(
    name = "Access Resolution",
    description = "Shared channel binding and access-resolution operations")
public class AccessResolutionController {

  private final ChannelBindingService channelBindingService;

  @PostMapping("/resolve")
  @PreAuthorize("hasRole('SERVICE') or hasRole('ADMIN')")
  @Operation(
      summary = "Resolve acting and represented creditor for a channel request",
      description =
          "Resolves the presented channel identity to an acting fordringshaver, optionally"
              + " with acting-on-behalf-of through parent-child hierarchy")
  public ResponseEntity<AccessResolutionResponse> resolveAccess(
      @Valid @RequestBody AccessResolutionRequest request) {
    AccessResolutionResponse response = channelBindingService.resolveAccess(request);
    if (!response.isAllowed()) {
      return ResponseEntity.status(403).body(response);
    }
    return ResponseEntity.ok(response);
  }
}
