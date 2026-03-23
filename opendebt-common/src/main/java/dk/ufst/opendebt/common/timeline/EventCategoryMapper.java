package dk.ufst.opendebt.common.timeline;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static utility for mapping raw event type strings to EventCategory and normalising aliases. Spec
 * §2.5 — 16-entry starter normalisation table.
 */
public final class EventCategoryMapper {

  private static final Logger log = LoggerFactory.getLogger(EventCategoryMapper.class);

  private EventCategoryMapper() {}

  /** Normalisation table: raw event type → canonical event type (for deduplication key). */
  private static final Map<String, String> NORMALISE =
      Map.ofEntries(
          Map.entry("CASE_CREATED", "CASE_CREATED"),
          Map.entry("CASE_STATUS_CHANGED", "CASE_STATUS_CHANGED"),
          Map.entry("CASE_ASSIGNED", "CASE_ASSIGNED"),
          Map.entry("DEBT_REGISTERED", "DEBT_REGISTERED"),
          Map.entry("DEBT_REGISTRATION", "DEBT_REGISTERED"), // payment alias → case canonical
          Map.entry("WRITEOFF", "DEBT_WRITEOFF"), // case alias → canonical
          Map.entry("DEBT_WRITEOFF", "DEBT_WRITEOFF"),
          Map.entry("PAYMENT_APPLIED", "PAYMENT_RECEIVED"), // case alias → canonical
          Map.entry("PAYMENT_RECEIVED", "PAYMENT_RECEIVED"),
          Map.entry("REFUND", "REFUND"),
          Map.entry("PARTIAL_PAYMENT", "PARTIAL_PAYMENT"),
          Map.entry("COLLECTION_MEASURE_INITIATED", "COLLECTION_MEASURE_INITIATED"),
          Map.entry("OBJECTION_FILED", "OBJECTION_FILED"),
          Map.entry("OBJECTION_OUTCOME", "OBJECTION_OUTCOME"),
          Map.entry("JOURNAL_ENTRY_ADDED", "JOURNAL_ENTRY_ADDED"),
          Map.entry("JOURNAL_NOTE_ADDED", "JOURNAL_NOTE_ADDED"));

  /** Category mapping for case-service event types (by canonical form). */
  private static final Map<String, EventCategory> CASE_CATEGORIES =
      Map.ofEntries(
          Map.entry("CASE_CREATED", EventCategory.CASE),
          Map.entry("CASE_STATUS_CHANGED", EventCategory.CASE),
          Map.entry("CASE_ASSIGNED", EventCategory.CASE),
          Map.entry("DEBT_REGISTERED", EventCategory.DEBT_LIFECYCLE),
          Map.entry("DEBT_WRITEOFF", EventCategory.DEBT_LIFECYCLE),
          Map.entry("PAYMENT_RECEIVED", EventCategory.FINANCIAL),
          Map.entry("COLLECTION_MEASURE_INITIATED", EventCategory.COLLECTION),
          Map.entry("OBJECTION_FILED", EventCategory.OBJECTION),
          Map.entry("OBJECTION_OUTCOME", EventCategory.OBJECTION),
          Map.entry("JOURNAL_ENTRY_ADDED", EventCategory.JOURNAL),
          Map.entry("JOURNAL_NOTE_ADDED", EventCategory.JOURNAL));

  /** Category mapping for payment-service event types (by canonical form). */
  private static final Map<String, EventCategory> PAYMENT_CATEGORIES =
      Map.ofEntries(
          Map.entry("DEBT_REGISTERED", EventCategory.DEBT_LIFECYCLE),
          Map.entry("DEBT_WRITEOFF", EventCategory.DEBT_LIFECYCLE),
          Map.entry("PAYMENT_RECEIVED", EventCategory.FINANCIAL),
          Map.entry("REFUND", EventCategory.FINANCIAL),
          Map.entry("PARTIAL_PAYMENT", EventCategory.FINANCIAL));

  /**
   * Maps a CaseEventDto.eventType to EventCategory. Returns EventCategory.CASE as safe default for
   * unmapped types (logs WARN).
   */
  public static EventCategory fromCaseEventType(String eventType) {
    String canonical = normalizeEventType(eventType);
    EventCategory cat = CASE_CATEGORIES.get(canonical);
    if (cat == null) {
      log.warn("Unknown case event type '{}' — defaulting to CASE category", eventType);
      return EventCategory.CASE;
    }
    return cat;
  }

  /**
   * Maps a DebtEventDto.eventType to EventCategory. Returns EventCategory.FINANCIAL as safe default
   * for unmapped types (logs WARN).
   */
  public static EventCategory fromDebtEventType(String eventType) {
    String canonical = normalizeEventType(eventType);
    EventCategory cat = PAYMENT_CATEGORIES.get(canonical);
    if (cat == null) {
      log.warn("Unknown debt event type '{}' — defaulting to FINANCIAL category", eventType);
      return EventCategory.FINANCIAL;
    }
    return cat;
  }

  /**
   * Normalises a raw event type string to its canonical form for deduplication key construction.
   * Returns the input unchanged if not found in the normalisation table.
   */
  public static String normalizeEventType(String eventType) {
    return NORMALISE.getOrDefault(eventType, eventType);
  }
}
