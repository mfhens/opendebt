package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.entity.BusinessConfigEntity;
import dk.ufst.opendebt.debtservice.repository.BusinessConfigRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves time-versioned business configuration values for a given effective date. Provides batch
 * pre-loading for use by batch jobs to minimize DB round-trips.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessConfigService {

  private final BusinessConfigRepository repository;

  /**
   * Cache for batch pre-loaded rates. Keyed by "configKey|effectiveDate". Cleared between batch
   * runs (not a long-lived cache).
   */
  private final Map<String, BigDecimal> batchCache = new ConcurrentHashMap<>();

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
    // Check batch cache first
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
    // Populate cache
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

  /** Exception thrown when no configuration entry matches the requested key and date. */
  public static class ConfigurationNotFoundException extends RuntimeException {
    public ConfigurationNotFoundException(String configKey, LocalDate effectiveDate) {
      super(
          String.format(
              "No business configuration found for key='%s' effective on %s",
              configKey, effectiveDate));
    }
  }
}
