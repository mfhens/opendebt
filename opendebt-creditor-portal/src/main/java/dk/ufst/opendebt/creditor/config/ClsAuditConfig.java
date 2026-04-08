package dk.ufst.opendebt.creditor.config;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ClsAuditConfig {

  @Bean
  @ConditionalOnMissingBean
  public ClsAuditClient clsAuditClient() {
    return new ClsAuditClient() {
      @Override
      public void shipEvent(ClsAuditEvent event) {
        log.debug("CLS disabled, audit event not shipped: {}", event.getEventId());
      }

      @Override
      public void shipEvents(List<ClsAuditEvent> events) {
        log.debug("CLS disabled, {} audit events not shipped", events.size());
      }

      @Override
      public void flush() {
        // No-op: disabled CLS client has no buffer to flush.
      }

      @Override
      public boolean isEnabled() {
        return false;
      }
    };
  }
}
