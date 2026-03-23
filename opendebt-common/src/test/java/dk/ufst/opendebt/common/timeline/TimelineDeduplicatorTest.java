package dk.ufst.opendebt.common.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TimelineDeduplicator}.
 *
 * <p>Covers petition050 AC-A2 (deduplication algorithm) and specs §2.6.
 *
 * <p>Gherkin scenarios covered:
 *
 * <ul>
 *   <li>Scenario: Duplicate events from overlapping sources are deduplicated (feature line 23)
 * </ul>
 *
 * <p>Spec §6.1 — TimelineDeduplicatorTest test cases.
 */
class TimelineDeduplicatorTest {

  private static final UUID DEBT_ID_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private static final UUID DEBT_ID_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
  private static final LocalDateTime TS = LocalDateTime.of(2026, 1, 10, 9, 30, 0);

  private TimelineEntryDto buildEntry(String dedupeKey, TimelineSource source, BigDecimal amount) {
    return TimelineEntryDto.builder()
        .id(UUID.randomUUID())
        .timestamp(TS)
        .eventCategory(EventCategory.FINANCIAL)
        .eventType("PAYMENT_RECEIVED")
        .title("timeline.event.title.PAYMENT_RECEIVED")
        .source(source)
        .dedupeKey(dedupeKey)
        .amount(amount)
        .build();
  }

  // ---------------------------------------------------------------------------
  // No-duplicate path
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-A2: deduplicate with no duplicates returns all entries unchanged")
  void deduplicate_noDuplicates_returnsAllEntries() {
    TimelineEntryDto entry1 =
        buildEntry("CASE_CREATED|CASE|2026-01-01T10:00", TimelineSource.CASE, null);
    TimelineEntryDto entry2 =
        buildEntry(
            "PAYMENT_RECEIVED|D-2001|2026-01-02T10:00", TimelineSource.PAYMENT, BigDecimal.TEN);
    TimelineEntryDto entry3 =
        buildEntry("DEBT_REGISTERED|D-2002|2026-01-03T10:00", TimelineSource.CASE, null);

    List<TimelineEntryDto> result =
        TimelineDeduplicator.deduplicate(List.of(entry1, entry2, entry3));
    assertThat(result).hasSize(3);
  }

  // ---------------------------------------------------------------------------
  // Deduplication with competing sources — amount=null for both → CASE wins
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-A2: deduplicate prefers CASE source when both entries have null amount")
  void deduplicate_bothAmountsNull_caseSourceEntryWins() {
    String key = "DEBT_REGISTERED|" + DEBT_ID_A + "|" + TS.truncatedTo(ChronoUnit.MINUTES);
    TimelineEntryDto caseEntry = buildEntry(key, TimelineSource.CASE, null);
    TimelineEntryDto paymentEntry = buildEntry(key, TimelineSource.PAYMENT, null);

    List<TimelineEntryDto> result =
        TimelineDeduplicator.deduplicate(List.of(caseEntry, paymentEntry));
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSource()).isEqualTo(TimelineSource.CASE);
    assertThat(result.get(0).getAmount()).isNull();
  }

  // ---------------------------------------------------------------------------
  // Deduplication — PAYMENT source wins when it carries amount data
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-A2: deduplicate prefers PAYMENT source when payment entry has non-null amount")
  void deduplicate_paymentEntryHasAmount_paymentSourceWins() {
    String key = "DEBT_REGISTERED|" + DEBT_ID_A + "|" + TS.truncatedTo(ChronoUnit.MINUTES);
    TimelineEntryDto caseEntry = buildEntry(key, TimelineSource.CASE, null);
    TimelineEntryDto paymentEntry = buildEntry(key, TimelineSource.PAYMENT, new BigDecimal("5000"));

    List<TimelineEntryDto> result =
        TimelineDeduplicator.deduplicate(List.of(caseEntry, paymentEntry));
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSource()).isEqualTo(TimelineSource.PAYMENT);
    assertThat(result.get(0).getAmount()).isEqualByComparingTo("5000");
  }

  // ---------------------------------------------------------------------------
  // Timestamp bucketing — 30 seconds apart (same minute) → deduplicated
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-A2: entries within the same timestamp minute bucket are deduplicated")
  void deduplicate_timestampsWithin30SecondsSameMinuteBucket_deduplicated() {
    LocalDateTime ts1 = LocalDateTime.of(2026, 1, 10, 9, 30, 0);
    LocalDateTime ts2 = LocalDateTime.of(2026, 1, 10, 9, 30, 30);
    String key1 = "PAYMENT_RECEIVED|" + DEBT_ID_A + "|" + ts1.truncatedTo(ChronoUnit.MINUTES);
    String key2 = "PAYMENT_RECEIVED|" + DEBT_ID_A + "|" + ts2.truncatedTo(ChronoUnit.MINUTES);
    // Both keys must be identical (same minute bucket)
    assertThat(key1).isEqualTo(key2);

    TimelineEntryDto e1 = buildEntry(key1, TimelineSource.CASE, null);
    TimelineEntryDto e2 = buildEntry(key2, TimelineSource.CASE, null);

    List<TimelineEntryDto> result = TimelineDeduplicator.deduplicate(List.of(e1, e2));
    assertThat(result).hasSize(1);
  }

  // ---------------------------------------------------------------------------
  // No dedup: different debtId → both entries retained
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-A2: entries with same event type but different debtId are NOT deduplicated")
  void deduplicate_differentDebtId_bothEntriesRetained() {
    String keyA = "DEBT_REGISTERED|" + DEBT_ID_A + "|" + TS.truncatedTo(ChronoUnit.MINUTES);
    String keyB = "DEBT_REGISTERED|" + DEBT_ID_B + "|" + TS.truncatedTo(ChronoUnit.MINUTES);
    TimelineEntryDto entry1 = buildEntry(keyA, TimelineSource.CASE, null);
    TimelineEntryDto entry2 = buildEntry(keyB, TimelineSource.CASE, null);

    List<TimelineEntryDto> result = TimelineDeduplicator.deduplicate(List.of(entry1, entry2));
    assertThat(result).hasSize(2);
  }

  // ---------------------------------------------------------------------------
  // No dedup: timestamps in different minute buckets → both entries retained
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-A2: entries with timestamps in different minute buckets are NOT deduplicated")
  void deduplicate_differentMinuteBuckets_bothEntriesRetained() {
    LocalDateTime ts1 = LocalDateTime.of(2026, 1, 10, 9, 30, 0);
    LocalDateTime ts2 = LocalDateTime.of(2026, 1, 10, 9, 31, 1);
    String key1 = "PAYMENT_RECEIVED|" + DEBT_ID_A + "|" + ts1.truncatedTo(ChronoUnit.MINUTES);
    String key2 = "PAYMENT_RECEIVED|" + DEBT_ID_A + "|" + ts2.truncatedTo(ChronoUnit.MINUTES);
    assertThat(key1).isNotEqualTo(key2);

    TimelineEntryDto e1 = buildEntry(key1, TimelineSource.CASE, null);
    TimelineEntryDto e2 = buildEntry(key2, TimelineSource.CASE, null);

    List<TimelineEntryDto> result = TimelineDeduplicator.deduplicate(List.of(e1, e2));
    assertThat(result).hasSize(2);
  }

  // ---------------------------------------------------------------------------
  // Pre-condition: null dedupeKey throws
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-A2: deduplicate throws IllegalArgumentException when any entry has null dedupeKey")
  void deduplicate_nullDedupeKey_throwsIllegalArgumentException() {
    TimelineEntryDto entryWithNullDedupeKey =
        TimelineEntryDto.builder()
            .id(UUID.randomUUID())
            .timestamp(TS)
            .eventCategory(EventCategory.CASE)
            .eventType("CASE_CREATED")
            .title("timeline.event.title.CASE_CREATED")
            .source(TimelineSource.CASE)
            .dedupeKey(null)
            .build();

    assertThatThrownBy(() -> TimelineDeduplicator.deduplicate(List.of(entryWithNullDedupeKey)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
