package dk.ufst.opendebt.gateway;

import javax.sql.DataSource;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Provides test-only beans: a no-op DataSource mock and a WireMock server for stubbing downstream
 * HTTP calls.
 *
 * <p>WireMock starts on port 9099 (matching {@code application-test.yml} service URLs) and is
 * managed by the Spring context lifecycle, ensuring a single server across all Cucumber scenarios.
 */
@TestConfiguration
@Profile("test")
public class TestDataSourceConfig {

  @Bean
  public DataSource dataSource() {
    return Mockito.mock(DataSource.class);
  }

  /**
   * Spring-managed WireMock server on port 9099. Started once with the test context; stopped on
   * context close via {@code destroyMethod}.
   */
  @Bean(destroyMethod = "stop")
  public WireMockServer wireMockServer() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9098));
    server.start();
    WireMock.configureFor("localhost", 9098);
    return server;
  }
}
