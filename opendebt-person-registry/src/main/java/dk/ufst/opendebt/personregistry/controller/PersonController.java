package dk.ufst.opendebt.personregistry.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.personregistry.dto.GdprExportResponse;
import dk.ufst.opendebt.personregistry.dto.PersonDto;
import dk.ufst.opendebt.personregistry.dto.PersonLookupRequest;
import dk.ufst.opendebt.personregistry.dto.PersonLookupResponse;
import dk.ufst.opendebt.personregistry.service.PersonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
@Tag(name = "Person Registry", description = "Centralized PII data management (internal API)")
public class PersonController {

  private final PersonService personService;

  @PostMapping("/lookup")
  @PreAuthorize("hasRole('SERVICE') or hasRole('ADMIN')")
  @Operation(
      summary = "Lookup or create person",
      description =
          "Returns technical UUID for a person. Creates if not exists. This is the primary API for other services.")
  public ResponseEntity<PersonLookupResponse> lookupOrCreate(
      @Valid @RequestBody PersonLookupRequest request) {
    return ResponseEntity.ok(personService.lookupOrCreate(request));
  }

  @GetMapping("/{personId}")
  @PreAuthorize("hasRole('SERVICE') or hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Get person details",
      description =
          "Returns full person details including PII. Only for authorized services/users.")
  public ResponseEntity<PersonDto> getPerson(@PathVariable UUID personId) {
    return ResponseEntity.ok(personService.getPersonById(personId));
  }

  @PutMapping("/{personId}")
  @PreAuthorize("hasRole('SERVICE') or hasRole('ADMIN')")
  @Operation(summary = "Update person details", description = "Updates PII for a person")
  public ResponseEntity<PersonDto> updatePerson(
      @PathVariable UUID personId, @Valid @RequestBody PersonDto personDto) {
    return ResponseEntity.ok(personService.updatePerson(personId, personDto));
  }

  @GetMapping("/{personId}/exists")
  @PreAuthorize("hasRole('SERVICE')")
  @Operation(
      summary = "Check if person exists",
      description = "Simple existence check without returning PII")
  public ResponseEntity<Boolean> exists(@PathVariable UUID personId) {
    return ResponseEntity.ok(personService.exists(personId));
  }

  @PostMapping("/{personId}/gdpr/export")
  @PreAuthorize("hasRole('GDPR_OFFICER') or hasRole('ADMIN')")
  @Operation(
      summary = "Export person data (GDPR)",
      description = "Exports all data held about a person for GDPR right to access")
  public ResponseEntity<GdprExportResponse> exportData(@PathVariable UUID personId) {
    return ResponseEntity.ok(personService.exportPersonData(personId));
  }

  @DeleteMapping("/{personId}/gdpr/erase")
  @PreAuthorize("hasRole('GDPR_OFFICER') or hasRole('ADMIN')")
  @Operation(
      summary = "Request data erasure (GDPR)",
      description = "Requests deletion of person data for GDPR right to erasure")
  public ResponseEntity<Void> requestErasure(
      @PathVariable UUID personId, @RequestParam String reason) {
    personService.requestDeletion(personId, reason);
    return ResponseEntity.accepted().build();
  }
}
