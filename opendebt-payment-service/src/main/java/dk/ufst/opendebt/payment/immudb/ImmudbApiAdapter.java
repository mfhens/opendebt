package dk.ufst.opendebt.payment.immudb;

/**
 * Internal adapter interface over the immudb4j {@code ImmuClient} API surface used by {@link
 * ImmuLedgerClient} (ADR-0029, TB-028).
 *
 * <p>This interface defines the three immudb4j operations required for the tamper-evidence
 * dual-write pattern. Implementations:
 *
 * <ul>
 *   <li>{@link RealImmudbAdapter} — wraps {@code io.codenotary.immudb4j.ImmuClient}; active when
 *       {@code opendebt.immudb.enabled=true}
 *   <li>{@link NoOpImmuLedgerAppender} — no-op bean active by default (disabled state)
 * </ul>
 *
 * <p>AIDEV-TODO (TB-029): Add health indicator and connection pooling once ADR-0029 is Accepted.
 *
 * <pre>
 * // Real immudb4j usage (for reference — TB-029 implementation):
 * ImmuClient client = ImmuClient.newBuilder()
 *     .withServerUrl(host)
 *     .withServerPort(port)
 *     .build();
 * client.openSession(database, username, password);
 * client.set(key, valueBytes);
 * client.closeSession();
 * </pre>
 */
public interface ImmudbApiAdapter {

  /**
   * Opens an authenticated session against the immudb server.
   *
   * <p>Maps to {@code ImmuClient.openSession(String database, String username, String password)}.
   */
  void openSession(String database, String username, String password) throws Exception;

  /**
   * Stores a key-value pair in immudb, producing a tamper-evident Merkle tree proof.
   *
   * <p>Maps to {@code ImmuClient.set(String key, byte[] value)}.
   */
  void set(String key, byte[] value) throws Exception;

  /** Closes the authenticated session. Maps to {@code ImmuClient.closeSession()}. */
  void closeSession() throws Exception;
}
