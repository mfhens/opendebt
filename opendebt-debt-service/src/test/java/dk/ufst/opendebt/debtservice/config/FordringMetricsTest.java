package dk.ufst.opendebt.debtservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("FordringMetrics")
class FordringMetricsTest {

  private SimpleMeterRegistry meterRegistry;
  private FordringMetrics metrics;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    metrics = new FordringMetrics(meterRegistry);
  }

  @Test
  @DisplayName("registers submissions counter on construction")
  void registersCounter() {
    Counter counter = meterRegistry.find("fordring_submissions_total").counter();

    assertThat(counter).isNotNull();
    assertThat(counter.count()).isZero();
  }

  @Test
  @DisplayName("recordSubmission increments the submissions counter")
  void recordSubmission() {
    metrics.recordSubmission();
    metrics.recordSubmission();
    metrics.recordSubmission();

    Counter counter = meterRegistry.find("fordring_submissions_total").counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(3.0);
  }
}
