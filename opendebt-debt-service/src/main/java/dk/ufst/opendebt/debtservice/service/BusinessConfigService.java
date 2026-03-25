package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.dto.config.ConfigCreationResult;
import dk.ufst.opendebt.debtservice.dto.config.ConfigEntryDto;
import dk.ufst.opendebt.debtservice.dto.config.CreateConfigRequest;
import dk.ufst.opendebt.debtservice.dto.config.UpdateConfigRequest;
import dk.ufst.opendebt.debtservice.entity.BusinessConfigAuditEntity;
import dk.ufst.opendebt.debtservice.entity.BusinessConfigEntity;
import dk.ufst.opendebt.debtservice.repository.BusinessConfigAuditRepository;
import dk.ufst.opendebt.debtservice.repository.BusinessConfigRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves time-versioned business configuration values for a given effective date. Provides batch
 * pre-loading for use by batch jobs to minimize DB round-trips. Also provides CRUD operations for
 * managing config entries with overlap detection, audit logging, and derived rate auto-computation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessConfigService {

  private final BusinessConfigRepository repository;
  private final BusinessConfigAuditRepository auditRepository;

  /**
   * Cache for batch pre-loaded rates. Keyed by "configKey|effectiveDate". Cleared between batch
   * runs (not a long-lived cache).
   */
  private final Map<String, BigDecimal> batchCache = new ConcurrentHashMap<>();

  private static final String ERR_ENTRY_NOT_FOUND = "Entry not found: ";
  private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
  private static final String VALUE_TYPE_DECIMAL = "DECIMAL";

  /** Warns at startup if the business_config table is empty (no rates configured). */
  @PostConstruct
  public void validateRequiredConfigs() {
    long count = repository.count();
    if (count == 0) {
      log.warn(
          "business_config table is empty — no rates or configuration values are loaded."
              + " Interest calculations will fall back to hardcoded defaults until the table is"
              + " seeded.");
    }
  }

  /**
   * Returns the decimal config value effective on the given date.
   *
   * @throws ConfigurationNotFoundException if no matching entry exists
   */
  @Transactional(readOnly = true)
  public BigDecimal getDecimalValue(String configKey, LocalDate effectiveDate) {
    String cacheKey = configKey + "|" + effectiveDate;
    BigDecimal cached = batchCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }

    List<BusinessConfigEntity> entries = repository.findEffective(configKey, effectiveDate);
    if (entries.isEmpty()) {
      throw new ConfigurationNotFoundException(configKey, effectiveDate);
    }
    BigDecimal value = entries.get(0).getDecimalValue();
    batchCache.put(cacheKey, value);
    return value;
  }

  /**
   * Pre-loads rates for multiple config keys at a given date into the batch cache. Returns a map of
   * configKey → value. Used by InterestAccrualJob to resolve all needed rates in a single pass.
   */
  @Transactional(readOnly = true)
  public Map<String, BigDecimal> preloadRatesForDate(
      LocalDate effectiveDate, List<String> configKeys) {
    batchCache.clear();
    Map<String, BigDecimal> result =
        configKeys.stream()
            .distinct()
            .collect(
                Collectors.toMap(
                    key -> key,
                    key -> {
                      List<BusinessConfigEntity> entries =
                          repository.findEffective(key, effectiveDate);
                      if (entries.isEmpty()) {
                        log.warn(
                            "No config found for key={} date={}, using ZERO", key, effectiveDate);
                        return BigDecimal.ZERO;
                      }
                      return entries.get(0).getDecimalValue();
                    }));
    result.forEach((key, value) -> batchCache.put(key + "|" + effectiveDate, value));
    return result;
  }

  /** Clears the batch cache. Call at start/end of batch runs. */
  public void clearCache() {
    batchCache.clear();
  }

  /** Returns the full version history for a config key. */
  @Transactional(readOnly = true)
  public List<BusinessConfigEntity> getHistory(String configKey) {
    return repository.findByConfigKeyOrderByValidFromDesc(configKey);
  }

  // ---------------------------------------------------------------------------
  // CRUD operations (P047-T2)
  // ---------------------------------------------------------------------------

  private ConfigEntryDto toDto(BusinessConfigEntity e) {
    return ConfigEntryDto.builder()
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
        .reviewStatus(e.getReviewStatus() != null ? e.getReviewStatus().name() : null)
        .computedStatus(e.getComputedStatus())
        .build();
  }

  /** Returns all config entries grouped by key, each group ordered by validFrom descending. */
  @Transactional(readOnly = true)
  public Map<String, List<ConfigEntryDto>> listAllGrouped() {
    List<String> keys = repository.findAllDistinctKeys();
    Map<String, List<ConfigEntryDto>> result = new LinkedHashMap<>();
    for (String key : keys) {
      List<ConfigEntryDto> entries =
          repository.findByConfigKeyOrderByValidFromDesc(key).stream().map(this::toDto).toList();
      result.put(key, entries);
    }
    return result;
  }

  /** Returns the effective config entry for a key on the given date as a DTO. */
  @Transactional(readOnly = true)
  public ConfigEntryDto getEffectiveEntry(String configKey, LocalDate effectiveDate) {
    List<BusinessConfigEntity> entries = repository.findEffective(configKey, effectiveDate);
    if (entries.isEmpty()) throw new ConfigurationNotFoundException(configKey, effectiveDate);
    return toDto(entries.get(0));
  }

  /**
   * Creates a new time-versioned config entry with overlap detection and optional auto-close of the
   * previous open-ended entry. When configKey is {@code RATE_NB_UDLAAN}, derived rates are also
   * auto-computed and returned in the result.
   */
  @Transactional
  public ConfigCreationResult createEntry(
      CreateConfigRequest req, String createdBy, boolean isAdmin) {
    if (!req.isSeedMigration() && req.getValidFrom().isBefore(LocalDate.now())) {
      throw new ConfigValidationException("Gyldig fra-dato kan ikke være i fortiden");
    }
    validateValueType(req.getConfigValue(), req.getValueType());
    if (req.getDescription() == null || req.getLegalBasis() == null) {
      throw new ConfigValidationException("Alle felter skal udfyldes");
    }

    LocalDate toDate = req.getValidTo() != null ? req.getValidTo() : LocalDate.of(9999, 12, 31);
    List<BusinessConfigEntity> overlapping =
        repository.findOverlapping(req.getConfigKey(), req.getValidFrom(), toDate);
    if (!overlapping.isEmpty()) {
      throw new ConfigValidationException("Gyldighedsperioden overlapper en eksisterende post");
    }

    List<BusinessConfigEntity> openEnded = repository.findOpenEnded(req.getConfigKey());
    for (BusinessConfigEntity existing : openEnded) {
      if (!existing.getValidFrom().isAfter(req.getValidFrom())) {
        existing.setValidTo(req.getValidFrom());
        repository.save(existing);
        log.info(
            "Auto-closed config entry id={} for key={} at valid_to={}",
            existing.getId(),
            req.getConfigKey(),
            req.getValidFrom());
      }
    }

    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .configKey(req.getConfigKey())
            .configValue(req.getConfigValue())
            .valueType(req.getValueType())
            .validFrom(req.getValidFrom())
            .validTo(req.getValidTo())
            .description(req.getDescription())
            .legalBasis(req.getLegalBasis())
            .createdBy(createdBy)
            .reviewStatus(
                isAdmin && req.isSeedMigration()
                    ? BusinessConfigEntity.ReviewStatus.APPROVED
                    : BusinessConfigEntity.ReviewStatus.PENDING_REVIEW)
            .build();
    entity = repository.save(entity);
    audit(
        entity.getId(),
        entity.getConfigKey(),
        "CREATE",
        null,
        entity.getConfigValue(),
        createdBy,
        null);
    log.info(
        "Created config entry: key={}, validFrom={}, by={}",
        entity.getConfigKey(),
        entity.getValidFrom(),
        createdBy);

    if ("RATE_NB_UDLAAN".equals(req.getConfigKey())) {
      List<ConfigEntryDto> derived =
          autoComputeDerivedRates(
              new BigDecimal(req.getConfigValue()), req.getValidFrom(), createdBy);
      return ConfigCreationResult.builder().created(toDto(entity)).derivedEntries(derived).build();
    }
    return ConfigCreationResult.builder().created(toDto(entity)).derivedEntries(List.of()).build();
  }

  /** Updates a FUTURE or PENDING_REVIEW config entry. Active/expired entries cannot be changed. */
  @Transactional
  public ConfigEntryDto updateEntry(UUID id, UpdateConfigRequest req, String performedBy) {
    BusinessConfigEntity entity =
        repository
            .findById(id)
            .orElseThrow(() -> new ConfigurationNotFoundException(ERR_ENTRY_NOT_FOUND + id));
    String computed = entity.getComputedStatus();
    if (!"FUTURE".equals(computed) && !STATUS_PENDING_REVIEW.equals(computed)) {
      throw new ConfigValidationException("Kun fremtidige eller afventende poster kan ændres");
    }
    if (req.getConfigValue() != null) {
      validateValueType(req.getConfigValue(), entity.getValueType());
    }
    String oldValue = entity.getConfigValue();
    if (req.getConfigValue() != null) entity.setConfigValue(req.getConfigValue());
    if (req.getValidTo() != null) entity.setValidTo(req.getValidTo());
    if (req.getDescription() != null) entity.setDescription(req.getDescription());
    if (req.getLegalBasis() != null) entity.setLegalBasis(req.getLegalBasis());
    if ("APPROVED".equals(req.getReviewStatus())) {
      entity.setReviewStatus(BusinessConfigEntity.ReviewStatus.APPROVED);
    }
    entity = repository.save(entity);
    audit(
        entity.getId(),
        entity.getConfigKey(),
        "UPDATE",
        oldValue,
        entity.getConfigValue(),
        performedBy,
        null);
    return toDto(entity);
  }

  /** Approves a PENDING_REVIEW entry, auto-closing any conflicting open-ended predecessor. */
  @Transactional
  public ConfigEntryDto approveEntry(UUID id, String performedBy) {
    BusinessConfigEntity entity =
        repository
            .findById(id)
            .orElseThrow(() -> new ConfigurationNotFoundException(ERR_ENTRY_NOT_FOUND + id));
    if (entity.getReviewStatus() != BusinessConfigEntity.ReviewStatus.PENDING_REVIEW) {
      throw new ConfigValidationException("Kun afventende poster kan godkendes");
    }
    entity.setReviewStatus(BusinessConfigEntity.ReviewStatus.APPROVED);
    List<BusinessConfigEntity> openEnded = repository.findOpenEnded(entity.getConfigKey());
    for (BusinessConfigEntity existing : openEnded) {
      if (!existing.getId().equals(entity.getId())
          && !existing.getValidFrom().isAfter(entity.getValidFrom())) {
        existing.setValidTo(entity.getValidFrom());
        repository.save(existing);
      }
    }
    entity = repository.save(entity);
    audit(
        entity.getId(),
        entity.getConfigKey(),
        "APPROVE",
        null,
        entity.getConfigValue(),
        performedBy,
        null);
    log.info("Approved config entry id={} key={} by={}", id, entity.getConfigKey(), performedBy);
    return toDto(entity);
  }

  /** Rejects and deletes a PENDING_REVIEW entry, recording an audit trail entry. */
  @Transactional
  public void rejectEntry(UUID id, String performedBy) {
    BusinessConfigEntity entity =
        repository
            .findById(id)
            .orElseThrow(() -> new ConfigurationNotFoundException(ERR_ENTRY_NOT_FOUND + id));
    if (entity.getReviewStatus() != BusinessConfigEntity.ReviewStatus.PENDING_REVIEW) {
      throw new ConfigValidationException("Kun afventende poster kan afvises");
    }
    audit(
        entity.getId(),
        entity.getConfigKey(),
        "REJECT",
        entity.getConfigValue(),
        null,
        performedBy,
        null);
    repository.delete(entity);
    log.info(
        "Rejected and deleted config entry id={} key={} by={}",
        id,
        entity.getConfigKey(),
        performedBy);
  }

  /** Deletes a FUTURE config entry. Only future (not yet active) entries may be removed. */
  @Transactional
  public void deleteEntry(UUID id, String performedBy) {
    BusinessConfigEntity entity =
        repository
            .findById(id)
            .orElseThrow(() -> new ConfigurationNotFoundException(ERR_ENTRY_NOT_FOUND + id));
    if (!"FUTURE".equals(entity.getComputedStatus())) {
      throw new ConfigValidationException("Kun fremtidige poster kan slettes");
    }
    audit(
        entity.getId(),
        entity.getConfigKey(),
        "DELETE",
        entity.getConfigValue(),
        null,
        performedBy,
        null);
    repository.delete(entity);
  }

  /** Returns the full audit trail for a config key, ordered by most-recent first. */
  @Transactional(readOnly = true)
  public List<BusinessConfigAuditEntity> getAuditTrail(String configKey) {
    return auditRepository.findByConfigKeyOrderByPerformedAtDesc(configKey);
  }

  // ---------------------------------------------------------------------------
  // Derived rate auto-computation (P047-T3)
  // ---------------------------------------------------------------------------

  /**
   * Auto-generates derived interest rates when a new NB rate is created. Returns the list of
   * auto-generated entries (all in PENDING_REVIEW status).
   */
  private List<ConfigEntryDto> autoComputeDerivedRates(
      BigDecimal nbRate, LocalDate validFrom, String createdBy) {

    Map<String, BigDecimal> derivedRates = new LinkedHashMap<>();
    derivedRates.put("RATE_INDR_STD", nbRate.add(new BigDecimal("0.04")));
    derivedRates.put("RATE_INDR_TOLD", nbRate.add(new BigDecimal("0.02")));
    derivedRates.put("RATE_INDR_TOLD_AFD", nbRate.add(new BigDecimal("0.01")));

    List<ConfigEntryDto> results = new ArrayList<>();
    for (Map.Entry<String, BigDecimal> entry : derivedRates.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue().setScale(4, RoundingMode.HALF_UP).toPlainString();

      List<BusinessConfigEntity> openEnded = repository.findOpenEnded(key);
      for (BusinessConfigEntity existing : openEnded) {
        if (!existing.getValidFrom().isAfter(validFrom)) {
          existing.setValidTo(validFrom);
          repository.save(existing);
        }
      }

      BusinessConfigEntity derived =
          BusinessConfigEntity.builder()
              .configKey(key)
              .configValue(value)
              .valueType(VALUE_TYPE_DECIMAL)
              .validFrom(validFrom)
              .reviewStatus(BusinessConfigEntity.ReviewStatus.PENDING_REVIEW)
              .description("Auto-beregnet fra RATE_NB_UDLAAN = " + nbRate.toPlainString())
              .legalBasis("Gældsinddrivelsesloven § 5, stk. 1-2")
              .createdBy("SYSTEM (auto-computed from RATE_NB_UDLAAN)")
              .build();

      derived = repository.save(derived);
      audit(
          derived.getId(),
          derived.getConfigKey(),
          "CREATE",
          null,
          derived.getConfigValue(),
          createdBy,
          "Auto-generated from RATE_NB_UDLAAN by " + createdBy);
      results.add(toDto(derived));
      log.info(
          "Auto-generated derived rate: key={}, value={}, validFrom={}", key, value, validFrom);
    }
    return results;
  }

  /** Returns a preview of what derived rates would be for a given NB rate (no side effects). */
  public List<ConfigEntryDto> previewDerivedRates(BigDecimal nbRate, LocalDate validFrom) {
    Map<String, BigDecimal> derived = new LinkedHashMap<>();
    derived.put("RATE_INDR_STD", nbRate.add(new BigDecimal("0.04")));
    derived.put("RATE_INDR_TOLD", nbRate.add(new BigDecimal("0.02")));
    derived.put("RATE_INDR_TOLD_AFD", nbRate.add(new BigDecimal("0.01")));
    return derived.entrySet().stream()
        .map(
            e ->
                ConfigEntryDto.builder()
                    .configKey(e.getKey())
                    .configValue(e.getValue().setScale(4, RoundingMode.HALF_UP).toPlainString())
                    .valueType(VALUE_TYPE_DECIMAL)
                    .validFrom(validFrom)
                    .computedStatus(STATUS_PENDING_REVIEW)
                    .reviewStatus(STATUS_PENDING_REVIEW)
                    .build())
        .toList();
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private void validateValueType(String value, String type) {
    try {
      switch (type) {
        case VALUE_TYPE_DECIMAL -> new BigDecimal(value);
        case "INTEGER" -> Integer.parseInt(value);
        case "BOOLEAN" -> {
          if (!"true".equals(value) && !"false".equals(value))
            throw new ConfigValidationException(
                "Værdien matcher ikke den forventede type (BOOLEAN)");
        }
        case "STRING" -> {
          if (value == null || value.isBlank())
            throw new ConfigValidationException(
                "Værdien matcher ikke den forventede type (STRING)");
        }
        default -> throw new ConfigValidationException("Ukendt value_type: " + type);
      }
    } catch (NumberFormatException e) {
      throw new ConfigValidationException(
          "Værdien matcher ikke den forventede type (" + type + ")");
    }
  }

  private void audit(
      UUID entryId,
      String key,
      String action,
      String oldVal,
      String newVal,
      String by,
      String details) {
    auditRepository.save(
        BusinessConfigAuditEntity.builder()
            .configEntryId(entryId)
            .configKey(key)
            .action(action)
            .oldValue(oldVal)
            .newValue(newVal)
            .performedBy(by)
            .performedAt(LocalDateTime.now())
            .details(details)
            .build());
  }

  // ---------------------------------------------------------------------------
  // Exception classes
  // ---------------------------------------------------------------------------

  /** Exception thrown when no configuration entry matches the requested key and date. */
  public static class ConfigurationNotFoundException extends RuntimeException {
    public ConfigurationNotFoundException(String configKey, LocalDate effectiveDate) {
      super(
          String.format(
              "No business configuration found for key='%s' effective on %s",
              configKey, effectiveDate));
    }

    public ConfigurationNotFoundException(String message) {
      super(message);
    }
  }

  /** Exception thrown when a config entry fails domain validation. */
  public static class ConfigValidationException extends RuntimeException {
    public ConfigValidationException(String message) {
      super(message);
    }
  }
}
