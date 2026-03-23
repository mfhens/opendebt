package dk.ufst.opendebt.common.timeline;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Normalised timeline entry DTO shared across all portals. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEntryDto {

  private UUID id;
  private LocalDateTime timestamp;
  private EventCategory eventCategory;
  private String eventType;

  /** i18n message key — resolved by Thymeleaf at render time via ${#messages.msg(entry.title)} */
  private String title;

  private String description;
  private BigDecimal amount;
  private UUID debtId;
  private String performedBy;
  private String metadata;

  /** Internal source indicator — never rendered in templates. */
  private TimelineSource source;

  /** Deduplication key — never rendered in templates. */
  private String dedupeKey;
}
