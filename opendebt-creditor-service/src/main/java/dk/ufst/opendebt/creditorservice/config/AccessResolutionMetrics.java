package dk.ufst.opendebt.creditorservice.config;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;

/** Custom business metrics for creditor access resolution (ADR-0024, petition020). */
@Component
@Getter
public class AccessResolutionMetrics {

  private final Counter allowedCounter;
  private final Counter deniedCounter;

  public AccessResolutionMetrics(MeterRegistry meterRegistry) {
    this.allowedCounter =
        Counter.builder("creditor_access_resolution_total")
            .description("Total access resolution outcomes")
            .tag("result", "allowed")
            .register(meterRegistry);
    this.deniedCounter =
        Counter.builder("creditor_access_resolution_total")
            .description("Total access resolution outcomes")
            .tag("result", "denied")
            .register(meterRegistry);
  }

  public void recordAllowed() {
    allowedCounter.increment();
  }

  public void recordDenied() {
    deniedCounter.increment();
  }
}
