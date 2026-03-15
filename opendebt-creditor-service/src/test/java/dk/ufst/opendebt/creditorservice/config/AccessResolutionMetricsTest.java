package dk.ufst.opendebt.creditorservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("AccessResolutionMetrics")
class AccessResolutionMetricsTest {

  private SimpleMeterRegistry meterRegistry;
  private AccessResolutionMetrics metrics;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    metrics = new AccessResolutionMetrics(meterRegistry);
  }

  @Test
  @DisplayName("registers allowed and denied counters on construction")
  void registersCounters() {
    Counter allowed =
        meterRegistry.find("creditor_access_resolution_total").tag("result", "allowed").counter();
    Counter denied =
        meterRegistry.find("creditor_access_resolution_total").tag("result", "denied").counter();

    assertThat(allowed).isNotNull();
    assertThat(denied).isNotNull();
    assertThat(allowed.count()).isZero();
    assertThat(denied.count()).isZero();
  }

  @Test
  @DisplayName("recordAllowed increments the allowed counter")
  void recordAllowed() {
    metrics.recordAllowed();
    metrics.recordAllowed();

    Counter allowed =
        meterRegistry.find("creditor_access_resolution_total").tag("result", "allowed").counter();
    assertThat(allowed).isNotNull();
    assertThat(allowed.count()).isEqualTo(2.0);
  }

  @Test
  @DisplayName("recordDenied increments the denied counter")
  void recordDenied() {
    metrics.recordDenied();

    Counter denied =
        meterRegistry.find("creditor_access_resolution_total").tag("result", "denied").counter();
    assertThat(denied).isNotNull();
    assertThat(denied.count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("allowed and denied counters are independent")
  void countersAreIndependent() {
    metrics.recordAllowed();
    metrics.recordDenied();
    metrics.recordAllowed();

    assertThat(metrics.getAllowedCounter().count()).isEqualTo(2.0);
    assertThat(metrics.getDeniedCounter().count()).isEqualTo(1.0);
  }
}
