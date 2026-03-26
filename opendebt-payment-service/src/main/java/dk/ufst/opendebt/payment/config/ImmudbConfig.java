package dk.ufst.opendebt.payment.config;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.ufst.opendebt.payment.immudb.ImmudbApiAdapter;
import dk.ufst.opendebt.payment.immudb.RealImmudbAdapter;

import io.codenotary.immudb4j.ImmuClient;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring configuration for the immudb tamper-evidence integration (ADR-0029, TB-028-b).
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
 * <p>AIDEV-TODO (TB-029): Replace single-session bean with a connection pool to support concurrent
 * async ledger appends safely (ImmuClient methods are synchronized internally).
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
   * Creates and authenticates a {@link RealImmudbAdapter} backed by {@link ImmuClient} (immudb4j).
   *
   * @return authenticated immudb adapter
   */
  @Bean
  public ImmudbApiAdapter immudbApiAdapter() {
    log.info("immudb: connecting to {}:{} database={}", host, port, database);
    ImmuClient client = ImmuClient.newBuilder().withServerUrl(host).withServerPort(port).build();
    adapter = new RealImmudbAdapter(client);
    try {
      adapter.openSession(database, username, password);
      log.info("immudb: connected and session opened at {}:{}", host, port);
    } catch (Exception e) {
      log.error(
          "immudb: failed to connect to {}:{} — tamper-evidence layer unavailable", host, port, e);
      throw new IllegalStateException("immudb connection failed on startup", e);
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
