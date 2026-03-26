package dk.ufst.opendebt.payment.immudb;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;

/**
 * Immutable JSON record stored in immudb for tamper-evidence verification (ADR-0029).
 *
 * <p>Mirrors the fields of {@link LedgerEntryEntity} that have legal and financial significance.
 * Serialised to JSON bytes and stored keyed by the entity's UUID.
 *
 * <p><b>GDPR:</b> contains no personal data. {@code debtId} is a system-internal UUID, not a CPR
 * number. Person-registry data must never be written to immudb (ADR-0029, GDPR constraint).
 *
 * <p><b>Currency:</b> OpenDebt operates exclusively in DKK (ADR-0018). The {@code currency} field
 * is included explicitly so an auditor can verify records without application context.
 *
 * <p>AIDEV-TODO: {@code contraAccountCode} is not carried on {@link LedgerEntryEntity}; it lives on
 * the paired entry sharing the same {@code transactionId}. TB-029 implementation should enrich this
 * field by querying the paired entry before appending, or by restructuring the append call to
 * receive both entries and cross-populate.
 */
public record LedgerImmuRecord(
    UUID transactionId,
    UUID entryId,
    UUID debtId,
    String entryType,
    String entryCategory,
    BigDecimal amount,
    String currency,
    String accountCode,
    String contraAccountCode,
    LocalDate valueDate,
    LocalDate postingDate,
    LocalDateTime createdAt) {

  // AIDEV-NOTE: Static ObjectMapper is safe here; ObjectMapper is thread-safe after configuration.
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  /**
   * Constructs a {@code LedgerImmuRecord} from a saved {@link LedgerEntryEntity}.
   *
   * <p>{@code contraAccountCode} is not populated here — it requires the paired entry. Set to
   * {@code null} for now; see AIDEV-TODO on this class.
   */
  public static LedgerImmuRecord from(LedgerEntryEntity entry) {
    return new LedgerImmuRecord(
        entry.getTransactionId(),
        entry.getId(),
        entry.getDebtId(),
        entry.getEntryType().name(),
        entry.getEntryCategory().name(),
        entry.getAmount(),
        "DKK",
        entry.getAccountCode(),
        null, // AIDEV-TODO: populate from paired entry (TB-029)
        entry.getEffectiveDate(),
        entry.getPostingDate(),
        entry.getCreatedAt());
  }

  /**
   * Serialises this record to a UTF-8 JSON byte array for storage in immudb.
   *
   * @throws IllegalStateException if Jackson serialisation fails
   */
  public byte[] toBytes() {
    try {
      return MAPPER.writeValueAsBytes(this);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to serialise LedgerImmuRecord for txn=" + transactionId + " entry=" + entryId, e);
    }
  }
}
