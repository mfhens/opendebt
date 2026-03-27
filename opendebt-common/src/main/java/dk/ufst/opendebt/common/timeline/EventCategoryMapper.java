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

  private static final String EVT_CASE_CREATED = "CASE_CREATED";
  private static final String EVT_CASE_STATUS_CHANGED = "CASE_STATUS_CHANGED";
  private static final String EVT_CASE_ASSIGNED = "CASE_ASSIGNED";
  private static final String EVT_DEBT_REGISTERED = "DEBT_REGISTERED";
  private static final String EVT_DEBT_WRITEOFF = "DEBT_WRITEOFF";
  private static final String EVT_PAYMENT_RECEIVED = "PAYMENT_RECEIVED";
  private static final String EVT_REFUND = "REFUND";
  private static final String EVT_PARTIAL_PAYMENT = "PARTIAL_PAYMENT";
  private static final String EVT_COLLECTION_MEASURE_INITIATED = "COLLECTION_MEASURE_INITIATED";
  private static final String EVT_OBJECTION_FILED = "OBJECTION_FILED";
  private static final String EVT_OBJECTION_OUTCOME = "OBJECTION_OUTCOME";
  private static final String EVT_JOURNAL_ENTRY_ADDED = "JOURNAL_ENTRY_ADDED";
  private static final String EVT_JOURNAL_NOTE_ADDED = "JOURNAL_NOTE_ADDED";

  /** Normalisation table: raw event type → canonical event type (for deduplication key). */
  private static final Map<String, String> NORMALISE =
      Map.ofEntries(
          Map.entry(EVT_CASE_CREATED, EVT_CASE_CREATED),
          Map.entry(EVT_CASE_STATUS_CHANGED, EVT_CASE_STATUS_CHANGED),
          Map.entry(EVT_CASE_ASSIGNED, EVT_CASE_ASSIGNED),
          Map.entry(EVT_DEBT_REGISTERED, EVT_DEBT_REGISTERED),
          Map.entry("DEBT_REGISTRATION", EVT_DEBT_REGISTERED), // payment alias → case canonical
          Map.entry("WRITEOFF", EVT_DEBT_WRITEOFF), // case alias → canonical
          Map.entry(EVT_DEBT_WRITEOFF, EVT_DEBT_WRITEOFF),
          Map.entry("PAYMENT_APPLIED", EVT_PAYMENT_RECEIVED), // case alias → canonical
          Map.entry(EVT_PAYMENT_RECEIVED, EVT_PAYMENT_RECEIVED),
          Map.entry(EVT_REFUND, EVT_REFUND),
          Map.entry(EVT_PARTIAL_PAYMENT, EVT_PARTIAL_PAYMENT),
          Map.entry(EVT_COLLECTION_MEASURE_INITIATED, EVT_COLLECTION_MEASURE_INITIATED),
          Map.entry(EVT_OBJECTION_FILED, EVT_OBJECTION_FILED),
          Map.entry(EVT_OBJECTION_OUTCOME, EVT_OBJECTION_OUTCOME),
          Map.entry(EVT_JOURNAL_ENTRY_ADDED, EVT_JOURNAL_ENTRY_ADDED),
          Map.entry(EVT_JOURNAL_NOTE_ADDED, EVT_JOURNAL_NOTE_ADDED));

  /** Category mapping for case-service event types (by canonical form). */
  private static final Map<String, EventCategory> CASE_CATEGORIES =
      Map.ofEntries(
          Map.entry(EVT_CASE_CREATED, EventCategory.CASE),
          Map.entry(EVT_CASE_STATUS_CHANGED, EventCategory.CASE),
          Map.entry(EVT_CASE_ASSIGNED, EventCategory.CASE),
          Map.entry(EVT_DEBT_REGISTERED, EventCategory.DEBT_LIFECYCLE),
          Map.entry(EVT_DEBT_WRITEOFF, EventCategory.DEBT_LIFECYCLE),
          Map.entry(EVT_PAYMENT_RECEIVED, EventCategory.FINANCIAL),
          Map.entry(EVT_COLLECTION_MEASURE_INITIATED, EventCategory.COLLECTION),
          Map.entry(EVT_OBJECTION_FILED, EventCategory.OBJECTION),
          Map.entry(EVT_OBJECTION_OUTCOME, EventCategory.OBJECTION),
          Map.entry(EVT_JOURNAL_ENTRY_ADDED, EventCategory.JOURNAL),
          Map.entry(EVT_JOURNAL_NOTE_ADDED, EventCategory.JOURNAL));

  /** Category mapping for payment-service event types (by canonical form). */
  private static final Map<String, EventCategory> PAYMENT_CATEGORIES =
      Map.ofEntries(
          Map.entry(EVT_DEBT_REGISTERED, EventCategory.DEBT_LIFECYCLE),
          Map.entry(EVT_DEBT_WRITEOFF, EventCategory.DEBT_LIFECYCLE),
          Map.entry(EVT_PAYMENT_RECEIVED, EventCategory.FINANCIAL),
          Map.entry(EVT_REFUND, EventCategory.FINANCIAL),
          Map.entry(EVT_PARTIAL_PAYMENT, EventCategory.FINANCIAL));

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
