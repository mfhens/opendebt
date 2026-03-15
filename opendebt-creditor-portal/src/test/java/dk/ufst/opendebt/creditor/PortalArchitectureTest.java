package dk.ufst.opendebt.creditor;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit architecture tests for the creditor-portal module.
 *
 * <p>Enforces trace propagation and layering rules aligned with ADR-0024 (Observability) and
 * project coding conventions.
 */
@AnalyzeClasses(
    packages = "dk.ufst.opendebt.creditor",
    importOptions = ImportOption.DoNotIncludeTests.class)
class PortalArchitectureTest {

  @ArchTest
  static final ArchRule clients_must_use_injected_webclient_builder =
      noClasses()
          .that()
          .resideInAPackage("..client..")
          .should()
          .callMethod(org.springframework.web.reactive.function.client.WebClient.class, "create")
          .orShould()
          .callMethod(
              org.springframework.web.reactive.function.client.WebClient.class,
              "create",
              String.class)
          .as(
              "Service clients must inject WebClient.Builder (Spring-managed) instead of calling "
                  + "WebClient.create(). The injected builder carries Micrometer Tracing filters "
                  + "for W3C Trace Context propagation (ADR-0024).");
}
