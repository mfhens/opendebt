package dk.ufst.opendebt.debtservice.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.debtservice.dto.config.ConfigCreationResult;
import dk.ufst.opendebt.debtservice.dto.config.ConfigEntryDto;
import dk.ufst.opendebt.debtservice.dto.config.CreateConfigRequest;
import dk.ufst.opendebt.debtservice.dto.config.UpdateConfigRequest;
import dk.ufst.opendebt.debtservice.entity.BusinessConfigAuditEntity;
import dk.ufst.opendebt.debtservice.service.BusinessConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API for managing versioned business configuration values. Implements petition 047 FR-1
 * endpoints. All write operations require ROLE_ADMIN or ROLE_CONFIGURATION_MANAGER.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class BusinessConfigController {

  private static final String SYSTEM_USER = "system";

  private final BusinessConfigService configService;

  /** GET /api/v1/config — list all config entries grouped by key. */
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER','CASEWORKER','SERVICE')")
  public ResponseEntity<Map<String, List<ConfigEntryDto>>> listAll() {
    return ResponseEntity.ok(configService.listAllGrouped());
  }

  /** GET /api/v1/config/{key}?date= — get effective value for a key and date. */
  @GetMapping("/{key}")
  @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER','CASEWORKER','SERVICE')")
  public ResponseEntity<ConfigEntryDto> getEffective(
      @PathVariable String key,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {
    LocalDate effectiveDate = date != null ? date : LocalDate.now();
    return ResponseEntity.ok(configService.getEffectiveEntry(key, effectiveDate));
  }

  /** GET /api/v1/config/{key}/history — get version history for a key. */
  @GetMapping("/{key}/history")
  @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER','CASEWORKER')")
  public ResponseEntity<List<ConfigEntryDto>> getHistory(@PathVariable String key) {
    return ResponseEntity.ok(
        configService.getHistory(key).stream()
            .map(
                e ->
                    ConfigEntryDto.builder()
                        .id(e.getId())
                        .configKey(e.getConfigKey())
                        .configValue(e.getConfigValue())
                        .valueType(e.getValueType())
                        .validFrom(e.getValidFrom())
                        .validTo(e.getValidTo())
                        .description(e.getDescription())
                        .legalBasis(e.getLegalBasis())
                        .createdBy(e.getCreatedBy())
                        .createdAt(e.getCreatedAt())
                        .reviewStatus(
                            e.getReviewStatus() != null ? e.getReviewStatus().name() : null)
                        .computedStatus(e.getComputedStatus())
                        .build())
            .toList());
  }

  /** GET /api/v1/config/{key}/audit — get audit trail for a key. */
  @GetMapping("/{key}/audit")
  @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER')")
  public ResponseEntity<List<BusinessConfigAuditEntity>> getAudit(@PathVariable String key) {
    return ResponseEntity.ok(configService.getAuditTrail(key));
  }

  /** GET /api/v1/config/{key}/preview?nbRate= — preview derived rates for an NB rate. */
  @GetMapping("/{key}/preview")
  @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER')")
  public ResponseEntity<List<ConfigEntryDto>> previewDerived(
      @PathVariable String key,
      @RequestParam BigDecimal nbRate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom) {
    return ResponseEntity.ok(configService.previewDerivedRates(nbRate, validFrom));
  }

  /** POST /api/v1/config — create a new config version. */
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER')")
  public ResponseEntity<ConfigCreationResult> create(
      @Valid @RequestBody CreateConfigRequest req, Principal principal) {
    String callerName = principal != null ? principal.getName() : SYSTEM_USER;
    boolean isAdmin = true; // role checked by @PreAuthorize; treat both as having seed rights
    ConfigCreationResult result = configService.createEntry(req, callerName, isAdmin);
    log.info("Config entry created: key={} by={}", req.getConfigKey(), callerName);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  /** PUT /api/v1/config/{id} — update a pending or future entry. */
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER')")
  public ResponseEntity<ConfigEntryDto> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateConfigRequest req, Principal principal) {
    String callerName = principal != null ? principal.getName() : SYSTEM_USER;
    return ResponseEntity.ok(configService.updateEntry(id, req, callerName));
  }

  /** PUT /api/v1/config/{id}/approve — approve a PENDING_REVIEW entry. */
  @PutMapping("/{id}/approve")
  @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER')")
  public ResponseEntity<ConfigEntryDto> approve(@PathVariable UUID id, Principal principal) {
    String callerName = principal != null ? principal.getName() : SYSTEM_USER;
    return ResponseEntity.ok(configService.approveEntry(id, callerName));
  }

  /** PUT /api/v1/config/{id}/reject — reject (delete) a PENDING_REVIEW entry. */
  @PutMapping("/{id}/reject")
  @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER')")
  public ResponseEntity<Void> reject(@PathVariable UUID id, Principal principal) {
    String callerName = principal != null ? principal.getName() : SYSTEM_USER;
    configService.rejectEntry(id, callerName);
    return ResponseEntity.noContent().build();
  }

  /** DELETE /api/v1/config/{id} — delete a future (not yet effective) entry. */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER')")
  public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
    String callerName = principal != null ? principal.getName() : SYSTEM_USER;
    configService.deleteEntry(id, callerName);
    return ResponseEntity.noContent().build();
  }

  /** Exception handler for validation errors. */
  @ExceptionHandler(BusinessConfigService.ConfigValidationException.class)
  public ResponseEntity<Map<String, String>> handleValidation(
      BusinessConfigService.ConfigValidationException ex) {
    return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
  }

  /** Exception handler for not found. */
  @ExceptionHandler(BusinessConfigService.ConfigurationNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(
      BusinessConfigService.ConfigurationNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
  }
}
