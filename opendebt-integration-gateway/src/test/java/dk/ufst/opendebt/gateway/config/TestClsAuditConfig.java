package dk.ufst.opendebt.gateway.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;

/**
 * Test configuration providing a no-op {@link ClsAuditClient} when no real implementation is
 * available (i.e., when {@code opendebt.audit.cls.enabled} is not {@code true}).
 *
 * <p>The no-op client collects events in memory so that Cucumber step definitions can assert CLS
 * audit event content via the {@code capturedEvents} list.
 *
 * <p>Traceability: SPEC-019-05 (ClsSoapAuditInterceptor), AC-13, AC-14, AC-15, AC-16, AC-34
 */
@Configuration
public class TestClsAuditConfig {

  /** Shared list of captured CLS events accessible from step definitions. */
  private static final List<ClsAuditEvent> capturedEvents = new ArrayList<>();

  /**
   * Returns the list of all captured CLS audit events. Reset between scenarios in {@code @Before}.
   */
  public static List<ClsAuditEvent> getCapturedEvents() {
    return capturedEvents;
  }

  /** Clears all captured events. Call from {@code @Before} in step definitions. */
  public static void resetCapturedEvents() {
    capturedEvents.clear();
  }

  @Bean
  @ConditionalOnMissingBean(ClsAuditClient.class)
  public ClsAuditClient noOpClsAuditClient() {
    return new ClsAuditClient() {
      @Override
      public void shipEvent(ClsAuditEvent event) {
        capturedEvents.add(event);
      }

      @Override
      public void shipEvents(List<ClsAuditEvent> events) {
        capturedEvents.addAll(events);
      }

      @Override
      public void flush() {}

      @Override
      public boolean isEnabled() {
        return true;
      }
    };
  }
}
