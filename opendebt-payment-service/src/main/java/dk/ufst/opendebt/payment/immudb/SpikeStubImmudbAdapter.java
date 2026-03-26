package dk.ufst.opendebt.payment.immudb;

import lombok.extern.slf4j.Slf4j;

/**
 * SPIKE STUB — TB-028 only. Replace with {@link RealImmudbAdapter} when immudb4j JAR is available.
 *
 * <p>Simulates the immudb4j {@code ImmuClient} API by logging the operations that WOULD be sent to
 * immudb. Allows the full dual-write pipeline to be compiled and tested in environments where the
 * {@code io.codenotary:immudb4j} JAR is not yet on the classpath.
 *
 * <p>This stub validates:
 *
 * <ul>
 *   <li>The conditional bean wiring (disabled-by-default pattern)
 *   <li>The async/retry call graph through {@link ImmuLedgerClient}
 *   <li>The {@link LedgerImmuRecord} JSON serialisation
 *   <li>The dual-write hook placement in {@link
 *       dk.ufst.opendebt.payment.bookkeeping.impl.BookkeepingServiceImpl}
 * </ul>
 *
 * <p>AIDEV-TODO (TB-028-b, TB-029): Replace with {@link RealImmudbAdapter} once:
 *
 * <ol>
 *   <li>{@code io.codenotary:immudb4j} is confirmed on Maven Central or proxied via UFST
 *       Artifactory/Nexus (see TB-028-b spike finding in docs/spike/TB-028-findings.md)
 *   <li>No classpath conflicts with reactor-netty / gRPC transitive deps are found
 *   <li>ADR-0029 is Accepted and TB-029 is created for production implementation
 * </ol>
 */
@Slf4j
public class SpikeStubImmudbAdapter implements ImmudbApiAdapter {

  private final String host;
  private final int port;
  private final String database;

  public SpikeStubImmudbAdapter(String host, int port, String database) {
    this.host = host;
    this.port = port;
    this.database = database;
  }

  @Override
  public void openSession(String database, String username, String password) {
    log.info(
        "immudb [SPIKE STUB]: openSession db={} user={} against {}:{}",
        database,
        username,
        host,
        port);
  }

  @Override
  public void set(String key, byte[] value) {
    log.info(
        "immudb [SPIKE STUB]: set key={} valueSize={}bytes — would write tamper-evident record",
        key,
        value.length);
  }

  @Override
  public void closeSession() {
    log.info("immudb [SPIKE STUB]: closeSession");
  }
}
