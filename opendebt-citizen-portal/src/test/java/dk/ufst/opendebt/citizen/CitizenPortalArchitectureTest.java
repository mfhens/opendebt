package dk.ufst.opendebt.citizen;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.springframework.web.reactive.function.client.WebClient;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import dk.ufst.opendebt.common.test.SharedArchRules;

@AnalyzeClasses(
    packages = "dk.ufst.opendebt.citizen",
    importOptions = ImportOption.DoNotIncludeTests.class)
class CitizenPortalArchitectureTest {

  @ArchTest
  static final ArchRule no_cross_service_db_access =
      SharedArchRules.noAccessToOtherServiceRepositories("citizen");

  @ArchTest
  static final ArchRule clients_must_use_injected_webclient_builder =
      noClasses()
          .that()
          .resideInAPackage("..client..")
          .should()
          .callMethod(WebClient.class, "create")
          .orShould()
          .callMethod(WebClient.class, "create", String.class)
          .as(
              "Citizen portal clients must inject WebClient.Builder instead of calling "
                  + "WebClient.create() so Micrometer tracing filters remain attached "
                  + "(ADR-0024, petition026).");
}
