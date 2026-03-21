package dk.ufst.opendebt.payment.bookkeeping.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;

import lombok.*;

/**
 * Immutable bi-temporal ledger entry representing one side of a double-entry posting.
 *
 * <p>Each financial transaction produces exactly two entries: one DEBIT and one CREDIT with the
 * same transactionId.
 *
 * <p>Bi-temporal model:
 *
 * <ul>
 *   <li>{@code effectiveDate} - when the economic event applies (value date)
 *   <li>{@code postingDate} - when the entry was recorded in the system
 * </ul>
 *
 * <p>Corrections use the storno pattern: reversal entries (STORNO) cancel the original, then new
 * correct entries are posted with the correct effectiveDate. Entries are never modified or deleted.
 */
@Entity
@Table(
    name = "ledger_entries",
    indexes = {
      @Index(name = "idx_ledger_transaction_id", columnList = "transaction_id"),
      @Index(name = "idx_ledger_debt_id", columnList = "debt_id"),
      @Index(name = "idx_ledger_account_code", columnList = "account_code"),
      @Index(name = "idx_ledger_created_at", columnList = "created_at"),
      @Index(name = "idx_ledger_effective_date", columnList = "effective_date"),
      @Index(name = "idx_ledger_posting_date", columnList = "posting_date"),
      @Index(name = "idx_ledger_reversal_of", columnList = "reversal_of_transaction_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "transaction_id", nullable = false)
  private UUID transactionId;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Column(name = "account_code", nullable = false, length = 10)
  private String accountCode;

  @Column(name = "account_name", nullable = false, length = 100)
  private String accountName;

  @Enumerated(EnumType.STRING)
  @Column(name = "entry_type", nullable = false, length = 10)
  private EntryType entryType;

  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  /** When the economic event applies (value date). */
  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  /** When this entry was recorded in the system. */
  @Column(name = "posting_date", nullable = false)
  private LocalDate postingDate;

  @Column(name = "reference", length = 200)
  private String reference;

  @Column(name = "description", length = 500)
  private String description;

  /** If this is a storno entry, references the transaction being reversed. */
  @Column(name = "reversal_of_transaction_id")
  private UUID reversalOfTransactionId;

  /** Category for grouping and filtering (e.g., INTEREST, PAYMENT, CORRECTION). */
  @Enumerated(EnumType.STRING)
  @Column(name = "entry_category", nullable = false, length = 20)
  private EntryCategory entryCategory;

  @CurrentTimestamp(event = EventType.INSERT, source = SourceType.VM)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public enum EntryType {
    DEBIT,
    CREDIT
  }

  public enum EntryCategory {
    DEBT_REGISTRATION,
    PAYMENT,
    INTEREST_ACCRUAL,
    OFFSETTING,
    WRITE_OFF,
    REFUND,
    STORNO,
    CORRECTION,
    COVERAGE_REVERSAL
  }
}
