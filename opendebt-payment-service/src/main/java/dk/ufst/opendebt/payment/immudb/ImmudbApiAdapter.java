package dk.ufst.opendebt.payment.immudb;

/**
 * Internal adapter interface over the immudb4j {@code ImmuClient} API surface used by {@link
 * ImmuLedgerClient} (ADR-0029, TB-028).
 *
 * <p>This interface defines the three immudb4j operations required for the tamper-evidence
 * dual-write pattern. Implementations:
 *
 * <ul>
 *   <li>{@link RealImmudbAdapter} — wraps the real {@code io.codenotary.immudb4j.ImmuClient}
 *       (requires immudb4j JAR; active in production once TB-028-b SDK validation passes)
 *   <li>{@link SpikeStubImmudbAdapter} — logs-only stub used for spike compilation verification and
 *       local dev when immudb4j JAR is not on the classpath
 * </ul>
 *
 * <p>AIDEV-NOTE: This façade decouples production code from the immudb4j compile-time classpath
 * requirement. Once ADR-0029 is Accepted and TB-029 begins, replace {@link SpikeStubImmudbAdapter}
 * with {@link RealImmudbAdapter} and add {@code io.codenotary:immudb4j} to the pom.xml.
 *
 * <p>API mirrors immudb4j's session-based client contract:
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
