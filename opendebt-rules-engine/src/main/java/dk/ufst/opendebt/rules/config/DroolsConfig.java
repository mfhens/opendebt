package dk.ufst.opendebt.rules.config;

import java.io.IOException;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

@Configuration
public class DroolsConfig {

  private static final String RULES_PATH = "rules/";

  @Bean
  public KieServices kieServices() {
    return KieServices.Factory.get();
  }

  @Bean
  public KieContainer kieContainer(KieServices kieServices) throws IOException {
    KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

    // Load all .drl and .xlsx files from rules directory
    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    // Load DRL files
    Resource[] drlResources = resolver.getResources("classpath*:" + RULES_PATH + "**/*.drl");
    for (Resource resource : drlResources) {
      kieFileSystem.write(
          ResourceFactory.newClassPathResource(RULES_PATH + resource.getFilename(), "UTF-8"));
    }

    // Load Excel decision tables
    Resource[] xlsResources = resolver.getResources("classpath*:" + RULES_PATH + "**/*.xlsx");
    for (Resource resource : xlsResources) {
      kieFileSystem.write(
          ResourceFactory.newClassPathResource(RULES_PATH + resource.getFilename(), "UTF-8"));
    }

    KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
    kieBuilder.buildAll();

    KieModule kieModule = kieBuilder.getKieModule();
    return kieServices.newKieContainer(kieModule.getReleaseId());
  }
}
