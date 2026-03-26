package dk.ufst.opendebt.payment.immudb;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * No-op fallback for {@link ImmuLedgerAppender}, active when immudb is disabled ({@code
 * opendebt.immudb.enabled} is false or absent).
 *
 * <p>Ensures {@link dk.ufst.opendebt.payment.bookkeeping.impl.BookkeepingServiceImpl} compiles and
 * operates normally without a running immudb instance — covering local dev, CI, and unit tests.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(ImmuLedgerAppender.class)
public class NoOpImmuLedgerAppender implements ImmuLedgerAppender {

  @Override
  public void appendAsync(LedgerEntryEntity... entries) {
    log.trace(
        "immudb disabled — skipping tamper-evidence append for {} ledger entries (txn={})",
        entries.length,
        entries.length > 0 ? entries[0].getTransactionId() : "?");
  }
}
