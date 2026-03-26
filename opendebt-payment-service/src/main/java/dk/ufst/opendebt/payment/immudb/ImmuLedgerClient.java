package dk.ufst.opendebt.payment.immudb;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Appends financial ledger entries to immudb for cryptographic tamper-evidence (ADR-0029).
 *
 * <p>Active only when {@code opendebt.immudb.enabled=true}. Uses the {@link ImmudbApiAdapter} bean
 * provided by {@link dk.ufst.opendebt.payment.config.ImmudbConfig}.
 *
 * <p><b>Async:</b> each append runs in a separate thread from the Spring task executor, decoupled
 * from the PostgreSQL {@code @Transactional} boundary. The PostgreSQL commit is never delayed or
 * rolled back by immudb availability.
 *
 * <p>AIDEV-TODO (TB-029): {@code @Async} + {@code @Retryable} interact in a non-obvious way:
 * Spring-Retry wraps the proxy call at submission time, but the async thread returns {@code void}
 * immediately — so retries fire at the submission level, not inside the async thread. For correct
 * retry-within-async behaviour, inject {@code RetryTemplate} and call it explicitly inside this
 * method body. Documented as a spike finding in {@code docs/spike/TB-028-findings.md}.
 *
 * <p>AIDEV-TODO (TB-029): The shared {@link ImmudbApiAdapter} session is not thread-safe for
 * concurrent async writes. Production implementation should use per-call sessions or a dedicated
 * connection pool to avoid race conditions under concurrent ledger postings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "opendebt.immudb.enabled", havingValue = "true")
public class ImmuLedgerClient implements ImmuLedgerAppender {

  private final ImmudbApiAdapter immudbAdapter;

  /**
   * Appends the given ledger entries to immudb asynchronously with exponential-backoff retry.
   *
   * <p>Each entry is stored under its entity UUID as key. The {@link LedgerImmuRecord} JSON payload
   * carries all financially significant fields for independent verification.
   *
   * @param entries debit and credit entries for a double-entry posting
   */
  @Async
  @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 500, multiplier = 2))
  @Override
  public void appendAsync(LedgerEntryEntity... entries) {
    log.debug(
        "immudb: appending {} entries for txn={}",
        entries.length,
        entries.length > 0 ? entries[0].getTransactionId() : "?");
    for (LedgerEntryEntity entry : entries) {
      try {
        LedgerImmuRecord record = LedgerImmuRecord.from(entry);
        immudbAdapter.set(entry.getId().toString(), record.toBytes());
        log.debug(
            "immudb: appended entry id={} txn={} type={}",
            entry.getId(),
            entry.getTransactionId(),
            entry.getEntryType());
      } catch (Exception e) {
        log.error(
            "immudb: failed to append entry id={} txn={} — will retry",
            entry.getId(),
            entry.getTransactionId(),
            e);
        throw new IllegalStateException("immudb append failed for entry " + entry.getId(), e);
      }
    }
    log.info(
        "immudb: tamper-evidence appended {} entries for txn={}",
        entries.length,
        entries.length > 0 ? entries[0].getTransactionId() : "?");
  }
}
