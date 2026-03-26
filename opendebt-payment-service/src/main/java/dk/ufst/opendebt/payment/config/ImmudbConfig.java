package dk.ufst.opendebt.payment.config;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.ufst.opendebt.payment.immudb.ImmudbApiAdapter;
import dk.ufst.opendebt.payment.immudb.SpikeStubImmudbAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring configuration for the immudb tamper-evidence integration (ADR-0029).
 *
 * <p>Active only when {@code opendebt.immudb.enabled=true}. When disabled (the default), this
 * entire configuration class is skipped — no {@link ImmudbApiAdapter} bean is created, no
 * connection to immudb is attempted, and {@link
 * dk.ufst.opendebt.payment.immudb.NoOpImmuLedgerAppender} takes over transparently.
 *
 * <p>Connection properties:
 *
 * <pre>
 * opendebt.immudb.enabled=true
 * opendebt.immudb.host=localhost        # immudb gRPC host
 * opendebt.immudb.port=3322             # immudb gRPC port (default 3322)
 * opendebt.immudb.username=immudb       # immudb username
 * opendebt.immudb.password=immudb       # immudb password
 * opendebt.immudb.database=defaultdb    # immudb database name
 * </pre>
 *
 * <p><b>TB-028 spike note:</b> this configuration currently creates a {@link
 * SpikeStubImmudbAdapter} that logs immudb operations without connecting to a real immudb instance.
 * Replace with {@link dk.ufst.opendebt.payment.immudb.RealImmudbAdapter} wrapping {@code
 * io.codenotary.immudb4j.ImmuClient} once TB-028-b SDK validation is complete.
 *
 * <p>AIDEV-TODO (TB-029): Replace {@link SpikeStubImmudbAdapter} with {@link
 * dk.ufst.opendebt.payment.immudb.RealImmudbAdapter} and add {@code io.codenotary:immudb4j} to
 * pom.xml after TB-028-b confirms: (1) immudb4j resolves from Maven Central or UFST Artifactory,
 * (2) no classpath conflicts exist, (3) ADR-0029 is Accepted.
 *
 * <p>AIDEV-TODO (TB-029): Replace single-session bean with a connection pool to support concurrent
 * async ledger appends safely.
 *
 * <p>AIDEV-TODO (TB-029): Add health indicator for immudb connection exposed via actuator {@code
 * /health} so monitoring can alert on immudb connectivity loss.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "opendebt.immudb.enabled", havingValue = "true")
public class ImmudbConfig {

  @Value("${opendebt.immudb.host}")
  private String host;

  @Value("${opendebt.immudb.port:3322}")
  private int port;

  @Value("${opendebt.immudb.username}")
  private String username;

  @Value("${opendebt.immudb.password}")
  private String password;

  @Value("${opendebt.immudb.database:defaultdb}")
  private String database;

  private ImmudbApiAdapter adapter;

  /**
   * Creates and authenticates an {@link ImmudbApiAdapter} bean against the configured immudb
   * instance.
   *
   * <p><b>Spike (TB-028):</b> returns a {@link SpikeStubImmudbAdapter} that logs operations.
   * <b>Production (TB-029):</b> replace with {@link
   * dk.ufst.opendebt.payment.immudb.RealImmudbAdapter} backed by {@code
   * io.codenotary.immudb4j.ImmuClient}.
   *
   * @return authenticated immudb adapter
   */
  @Bean
  public ImmudbApiAdapter immudbApiAdapter() {
    log.info("immudb: initialising adapter for {}:{} database={}", host, port, database);
    // AIDEV-TODO (TB-029): Replace SpikeStubImmudbAdapter with:
    //   RealImmudbAdapter wrapping ImmuClient.newBuilder()
    //                                       .withServerUrl(host)
    //                                       .withServerPort(port)
    //                                       .build()
    //   then call adapter.openSession(database, username, password)
    adapter = new SpikeStubImmudbAdapter(host, port, database);
    try {
      adapter.openSession(database, username, password);
      log.info("immudb: adapter initialised (spike stub) for {}:{}", host, port);
    } catch (Exception e) {
      log.error(
          "immudb: failed to initialise adapter for {}:{} — tamper-evidence layer unavailable",
          host,
          port,
          e);
      // AIDEV-TODO (TB-029): Decide whether startup failure should be fatal or degrade gracefully.
      // For the spike, we throw to surface connectivity issues early.
      throw new IllegalStateException("immudb adapter initialisation failed on startup", e);
    }
    return adapter;
  }

  /** Closes the immudb session when the Spring application context shuts down. */
  @PreDestroy
  public void closeImmuSession() {
    if (adapter != null) {
      try {
        adapter.closeSession();
        log.info("immudb: session closed");
      } catch (Exception e) {
        log.warn("immudb: error closing session on shutdown", e);
      }
    }
  }
}
