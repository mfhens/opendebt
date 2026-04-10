package dk.ufst.rules.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring-agnostic factory for creating a {@link KieContainer} from classpath DRL resources.
 *
 * <p>Loads all known DRL rules bundled with {@code ufst-rules-lib} from the classpath. Use {@link
 * #buildFromClasspath()} in Spring {@code @Configuration} classes to expose the container as a
 * managed bean.
 */
@Slf4j
public final class KieContainerFactory {

  private static final String RULES_PATH = "rules/";

  /** Known DRL paths bundled with ufst-rules-lib. */
  private static final String[] DRL_PATHS = {
    RULES_PATH + "interest-calculation.drl",
    RULES_PATH + "debt-readiness.drl",
    RULES_PATH + "collection-priority.drl",
    RULES_PATH + "fordring/fordring-validation.drl",
  };

  private KieContainerFactory() {}

  /**
   * Builds a {@link KieContainer} by loading all bundled DRL files from the classpath.
   *
   * @return a fully-built KieContainer ready for session creation
   * @throws IllegalStateException if the DRL compilation produces errors
   */
  public static KieContainer buildFromClasspath() {
    KieServices kieServices = KieServices.Factory.get();
    KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

    for (String path : DRL_PATHS) {
      log.debug("Loading DRL: {}", path);
      kieFileSystem.write(ResourceFactory.newClassPathResource(path, "UTF-8"));
    }

    KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
    kieBuilder.buildAll();

    long errorCount =
        kieBuilder.getResults().getMessages().stream()
            .filter(m -> m.getLevel() == Message.Level.ERROR)
            .count();
    if (errorCount > 0) {
      kieBuilder.getResults().getMessages().stream()
          .filter(m -> m.getLevel() == Message.Level.ERROR)
          .forEach(m -> log.error("DRL compilation error: {}", m.getText()));
      throw new IllegalStateException(
          "KieContainer build failed with " + errorCount + " DRL compilation error(s)");
    }

    KieModule kieModule = kieBuilder.getKieModule();
    return kieServices.newKieContainer(kieModule.getReleaseId());
  }
}
