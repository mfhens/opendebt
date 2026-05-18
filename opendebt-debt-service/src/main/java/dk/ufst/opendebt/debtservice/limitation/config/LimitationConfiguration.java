package dk.ufst.opendebt.debtservice.limitation.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LimitationConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = "limitationClock")
  public Clock limitationClock() {
    return Clock.systemDefaultZone();
  }
}
