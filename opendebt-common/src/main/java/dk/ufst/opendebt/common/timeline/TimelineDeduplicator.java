package dk.ufst.opendebt.common.timeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Static utility for deduplicating merged timeline entries. Spec §2.6 — PAYMENT source wins over
 * CASE when it carries financial data (amount != null).
 */
public final class TimelineDeduplicator {

  private TimelineDeduplicator() {}

  /**
   * Deduplicates a list of timeline entries using the dedupeKey field.
   *
   * <p>Algorithm: 1. Iterate entries in order (caller must supply case-service entries first). 2.
   * Maintain a LinkedHashMap keyed on dedupeKey. 3. PAYMENT source entry overwrites CASE source
   * entry only when the incoming entry has a non-null amount and the stored entry has a null
   * amount. 4. Otherwise the first entry seen wins (CASE wins when both amounts are null).
   *
   * @param entries list to deduplicate; must not contain null dedupeKey values
   * @return deduplicated list preserving insertion order
   * @throws IllegalArgumentException if any entry has a null dedupeKey
   */
  public static List<TimelineEntryDto> deduplicate(List<TimelineEntryDto> entries) {
    LinkedHashMap<String, TimelineEntryDto> seen = new LinkedHashMap<>();
    for (TimelineEntryDto entry : entries) {
      if (entry.getDedupeKey() == null) {
        throw new IllegalArgumentException(
            "TimelineEntryDto.dedupeKey must not be null — entry id: " + entry.getId());
      }
      String key = entry.getDedupeKey();
      if (!seen.containsKey(key)) {
        seen.put(key, entry);
      } else {
        TimelineEntryDto existing = seen.get(key);
        if (existing.getAmount() == null && entry.getAmount() != null) {
          // PAYMENT source has richer financial data — overwrite
          seen.put(key, entry);
        }
        // Otherwise: discard incoming (CASE wins when both null, first seen wins otherwise)
      }
    }
    return new ArrayList<>(seen.values());
  }
}
