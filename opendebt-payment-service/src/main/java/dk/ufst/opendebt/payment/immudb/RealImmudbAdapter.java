package dk.ufst.opendebt.payment.immudb;

import io.codenotary.immudb4j.ImmuClient;
import lombok.extern.slf4j.Slf4j;

/**
 * Production adapter wrapping {@code io.codenotary.immudb4j.ImmuClient} (ADR-0029, TB-028-b).
 *
 * <p>Implements the three immudb4j operations required for the dual-write tamper-evidence pattern.
 * Each {@link #set} call produces a cryptographically verifiable Merkle tree proof in immudb.
 *
 * <p>Thread-safety note: {@code ImmuClient} methods are {@code synchronized} internally. AIDEV-TODO
 * (TB-029): Replace single shared client with a connection pool for high-throughput concurrent
 * async ledger appends.
 */
@Slf4j
public class RealImmudbAdapter implements ImmudbApiAdapter {

  private final ImmuClient client;

  public RealImmudbAdapter(ImmuClient client) {
    this.client = client;
  }

  @Override
  public void openSession(String database, String username, String password) throws Exception {
    client.openSession(database, username, password);
    log.info("immudb: session opened database={}", database);
  }

  @Override
  public void set(String key, byte[] value) throws Exception {
    client.set(key, value);
  }

  @Override
  public void closeSession() throws Exception {
    client.closeSession();
  }
}
