package dk.ufst.opendebt.payment.bookkeeping.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

/**
 * Immutable event in the debt timeline. Events are the source of truth for reconstructing the
 * financial state of a debt at any point in time. When a retroactive correction arrives, a new
 * CORRECTION event is appended (never modifying previous events), and downstream effects (interest)
 * are recalculated.
 */
@Entity
@Table(
    name = "debt_events",
    indexes = {
      @Index(name = "idx_debt_event_debt_id", columnList = "debt_id"),
      @Index(name = "idx_debt_event_effective_date", columnList = "effective_date"),
      @Index(name = "idx_debt_event_type", columnList = "event_type")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtEventEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 30)
  private EventType eventType;

  /** When the event economically applies (can be in the past for retroactive corrections). */
  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  /** The amount associated with this event (principal, payment, adjustment, etc.). */
  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  /** Reference to original event being corrected (for CORRECTION events). */
  @Column(name = "corrects_event_id")
  private UUID correctsEventId;

  @Column(name = "reference", length = 200)
  private String reference;

  @Column(name = "description", length = 500)
  private String description;

  /** The ledger transaction_id produced by processing this event. */
  @Column(name = "ledger_transaction_id")
  private UUID ledgerTransactionId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public enum EventType {
    DEBT_REGISTERED,
    PAYMENT_RECEIVED,
    INTEREST_ACCRUED,
    OFFSETTING_EXECUTED,
    WRITE_OFF,
    REFUND,
    UDLAEG_REGISTERED,
    UDLAEG_CORRECTED,
    CORRECTION
  }
}
