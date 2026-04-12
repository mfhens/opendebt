package dk.ufst.opendebt.debtservice.config;

import org.kie.api.runtime.KieContainer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.ufst.rules.config.KieContainerFactory;
import dk.ufst.rules.service.FordringValidationService;
import dk.ufst.rules.service.RulesService;
import dk.ufst.rules.service.impl.FordringValidationServiceImpl;
import dk.ufst.rules.service.impl.RulesServiceImpl;

/**
 * Wires the embedded ufst-rules-lib into the debt-service Spring context (TB-057).
 *
 * <p>Provides a singleton {@link KieContainer} (expensive to build) and a {@link RulesService}
 * backed by it. The container is built once at startup from the DRL files bundled in
 * ufst-rules-lib.
 */
@Configuration
public class DroolsAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public KieContainer kieContainer() {
    return KieContainerFactory.buildFromClasspath();
  }

  @Bean
  @ConditionalOnMissingBean
  public RulesService rulesService(KieContainer kieContainer) {
    return new RulesServiceImpl(kieContainer);
  }

  @Bean
  @ConditionalOnMissingBean
  public FordringValidationService fordringValidationService(KieContainer kieContainer) {
    return new FordringValidationServiceImpl(kieContainer);
  }
}
