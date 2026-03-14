package dk.ufst.opendebt.personregistry.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.personregistry.dto.OrganizationDto;
import dk.ufst.opendebt.personregistry.dto.OrganizationLookupRequest;
import dk.ufst.opendebt.personregistry.dto.OrganizationLookupResponse;
import dk.ufst.opendebt.personregistry.service.OrganizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(
    name = "Organizations",
    description = "Organization identity operations for internal services (W1-BOOT-02)")
public class OrganizationController {

  private final OrganizationService organizationService;

  @PostMapping("/lookup")
  @PreAuthorize("hasRole('SERVICE') or hasRole('ADMIN')")
  @Operation(
      summary = "Lookup or create organization by CVR",
      description =
          "Returns technical UUID for an organization. Creates if not exists. Primary API for creditor-service.")
  public ResponseEntity<OrganizationLookupResponse> lookupOrCreate(
      @Valid @RequestBody OrganizationLookupRequest request) {
    return ResponseEntity.ok(organizationService.lookupOrCreate(request));
  }

  @GetMapping("/{organizationId}")
  @PreAuthorize("hasRole('SERVICE') or hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Get organization details by technical identifier",
      description =
          "Returns full organization details including CVR. Only for authorized services/users.")
  public ResponseEntity<OrganizationDto> getOrganization(@PathVariable UUID organizationId) {
    return ResponseEntity.ok(organizationService.getOrganizationById(organizationId));
  }

  @GetMapping("/{organizationId}/exists")
  @PreAuthorize("hasRole('SERVICE')")
  @Operation(
      summary = "Check whether an organization exists",
      description = "Simple existence check without returning sensitive data")
  public ResponseEntity<Boolean> exists(@PathVariable UUID organizationId) {
    return ResponseEntity.ok(organizationService.exists(organizationId));
  }
}
