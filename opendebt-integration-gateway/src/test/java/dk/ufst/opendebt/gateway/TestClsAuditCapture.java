package dk.ufst.opendebt.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;

/**
 * Test-only CLS audit client that captures events for assertions. Replaces NoOpClsAuditClient in
 * test profile via @Primary.
 */
@Component
@Primary
@Profile("test")
public class TestClsAuditCapture implements ClsAuditClient {

  private final List<ClsAuditEvent> capturedEvents = new CopyOnWriteArrayList<>();

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

  public List<ClsAuditEvent> getCapturedEvents() {
    return new ArrayList<>(capturedEvents);
  }

  public ClsAuditEvent getLastEvent() {
    List<ClsAuditEvent> events = getCapturedEvents();
    return events.isEmpty() ? null : events.get(events.size() - 1);
  }

  public void reset() {
    capturedEvents.clear();
  }
}
