package dk.ufst.opendebt.debtservice.config;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;

/** Custom business metrics for fordring (claim) submissions (ADR-0024, petition020). */
@Component
@Getter
public class FordringMetrics {

  private final Counter submissionsCounter;

  public FordringMetrics(MeterRegistry meterRegistry) {
    this.submissionsCounter =
        Counter.builder("fordring_submissions_total")
            .description("Total fordring submissions")
            .register(meterRegistry);
  }

  public void recordSubmission() {
    submissionsCounter.increment();
  }
}
