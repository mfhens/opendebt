package dk.ufst.rules.test;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;

/**
 * Test support for isolated Drools rule evaluation.
 *
 * <p>Provides helpers to create a {@link KieContainer} scoped to a single DRL file, avoiding
 * global-variable conflicts when only one rule file is under test.
 */
public final class RulesTestHarness {

  private RulesTestHarness() {}

  /**
   * Builds a {@link KieContainer} from a single DRL file on the classpath.
   *
   * @param drlClasspathPath classpath-relative path, e.g. {@code "rules/interest-calculation.drl"}
   * @return a compiled KieContainer ready for session creation
   * @throws IllegalStateException if the DRL fails to compile
   */
  public static KieContainer buildSingleDrl(String drlClasspathPath) {
    KieServices kieServices = KieServices.Factory.get();
    KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

    kieFileSystem.write(ResourceFactory.newClassPathResource(drlClasspathPath, "UTF-8"));

    KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
    kieBuilder.buildAll();

    long errors =
        kieBuilder.getResults().getMessages().stream()
            .filter(m -> m.getLevel() == org.kie.api.builder.Message.Level.ERROR)
            .count();
    if (errors > 0) {
      throw new IllegalStateException(
          "DRL compilation failed for " + drlClasspathPath + ": " + errors + " error(s)");
    }

    KieModule kieModule = kieBuilder.getKieModule();
    return kieServices.newKieContainer(kieModule.getReleaseId());
  }
}
