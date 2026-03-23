package dk.ufst.opendebt.common.timeline;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import dk.ufst.opendebt.common.dto.CaseEventDto;
import dk.ufst.opendebt.common.dto.DebtEventDto;

/** Static utility for mapping source DTOs to TimelineEntryDto. Spec §2.3. */
public final class TimelineEntryMapper {

  private TimelineEntryMapper() {}

  /** Maps a CaseEventDto to a TimelineEntryDto. Spec §2.3 mapping rules — from CaseEventDto. */
  public static TimelineEntryDto fromCaseEvent(CaseEventDto event) {
    String eventType = event.getEventType();
    String normalized = EventCategoryMapper.normalizeEventType(eventType);
    EventCategory category = EventCategoryMapper.fromCaseEventType(eventType);
    String title = "timeline.event.title." + normalized;
    LocalDateTime timestamp = event.getPerformedAt();
    String dedupeKey =
        normalized
            + "|"
            + (event.getDebtId() != null ? event.getDebtId().toString() : "CASE")
            + "|"
            + (timestamp != null ? timestamp.truncatedTo(ChronoUnit.MINUTES) : "null");

    return TimelineEntryDto.builder()
        .id(event.getId())
        .timestamp(timestamp)
        .eventCategory(category)
        .eventType(eventType)
        .title(title)
        .description(event.getDescription())
        .amount(null)
        .debtId(null)
        .performedBy(event.getPerformedBy())
        .metadata(event.getMetadata())
        .source(TimelineSource.CASE)
        .dedupeKey(dedupeKey)
        .build();
  }

  /** Maps a DebtEventDto to a TimelineEntryDto. Spec §2.3 mapping rules — from DebtEventDto. */
  public static TimelineEntryDto fromDebtEvent(DebtEventDto event) {
    String eventType = event.getEventType();
    String normalized = EventCategoryMapper.normalizeEventType(eventType);
    EventCategory category = EventCategoryMapper.fromDebtEventType(eventType);
    String title = "timeline.event.title." + normalized;

    // timestamp: createdAt if non-null; else effectiveDate.atStartOfDay()
    LocalDateTime timestamp =
        event.getCreatedAt() != null
            ? event.getCreatedAt()
            : (event.getEffectiveDate() != null ? event.getEffectiveDate().atStartOfDay() : null);

    // metadata: compose reference and correctsEventId
    String metadata = null;
    if (event.getReference() != null) {
      metadata = "ref:" + event.getReference();
      if (event.getCorrectsEventId() != null) {
        metadata += " corrects:" + event.getCorrectsEventId();
      }
    } else if (event.getCorrectsEventId() != null) {
      metadata = "corrects:" + event.getCorrectsEventId();
    }

    String dedupeKey =
        normalized
            + "|"
            + (event.getDebtId() != null ? event.getDebtId().toString() : "CASE")
            + "|"
            + (timestamp != null ? timestamp.truncatedTo(ChronoUnit.MINUTES) : "null");

    return TimelineEntryDto.builder()
        .id(event.getId())
        .timestamp(timestamp)
        .eventCategory(category)
        .eventType(eventType)
        .title(title)
        .description(event.getDescription())
        .amount(event.getAmount())
        .debtId(event.getDebtId())
        .performedBy(null)
        .metadata(metadata)
        .source(TimelineSource.PAYMENT)
        .dedupeKey(dedupeKey)
        .build();
  }
}
