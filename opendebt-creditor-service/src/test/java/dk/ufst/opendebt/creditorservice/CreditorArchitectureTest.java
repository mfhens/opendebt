package dk.ufst.opendebt.creditorservice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import jakarta.persistence.Entity;

import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit architecture tests for the creditor-service module.
 *
 * <p>Enforces layered architecture rules aligned with ADR-0002 (Microservices Architecture),
 * ADR-0007 (No Cross-Service Database Connections), and ADR-0014 (GDPR Data Isolation).
 */
@AnalyzeClasses(
    packages = "dk.ufst.opendebt.creditorservice",
    importOptions = ImportOption.DoNotIncludeTests.class)
class CreditorArchitectureTest {

  // ========================================================================================
  // Layered architecture
  // ========================================================================================

  @ArchTest
  static final ArchRule layered_architecture_is_respected =
      layeredArchitecture()
          .consideringAllDependencies()
          .layer("Controller")
          .definedBy("..controller..")
          .layer("Service")
          .definedBy("..service..")
          .layer("Repository")
          .definedBy("..repository..")
          .layer("DTO")
          .definedBy("..dto..")
          .layer("Entity")
          .definedBy("..entity..")
          .layer("Mapper")
          .definedBy("..mapper..")
          .whereLayer("Controller")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Repository")
          .mayOnlyBeAccessedByLayers("Service");

  // ========================================================================================
  // Controller rules
  // ========================================================================================

  @ArchTest
  static final ArchRule controllers_should_not_access_repositories =
      noClasses()
          .that()
          .resideInAPackage("..controller..")
          .should()
          .accessClassesThat()
          .resideInAPackage("..repository..");

  @ArchTest
  static final ArchRule controllers_should_be_annotated =
      classes()
          .that()
          .resideInAPackage("..controller..")
          .and()
          .haveSimpleNameEndingWith("Controller")
          .should()
          .beAnnotatedWith(RestController.class);

  // ========================================================================================
  // Service rules
  // ========================================================================================

  @ArchTest
  static final ArchRule service_implementations_should_reside_in_impl_package =
      classes()
          .that()
          .areAnnotatedWith(Service.class)
          .should()
          .resideInAPackage("..service.impl..");

  @ArchTest
  static final ArchRule service_interfaces_should_not_depend_on_entities =
      noClasses()
          .that()
          .resideInAPackage("..service")
          .and()
          .areInterfaces()
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..entity..");

  // ========================================================================================
  // Repository rules
  // ========================================================================================

  @ArchTest
  static final ArchRule repositories_should_be_annotated =
      classes()
          .that()
          .resideInAPackage("..repository..")
          .and()
          .haveSimpleNameEndingWith("Repository")
          .should()
          .beAnnotatedWith(Repository.class);

  // ========================================================================================
  // DTO rules
  // ========================================================================================

  @ArchTest
  static final ArchRule dtos_should_not_have_jpa_annotations =
      noClasses().that().resideInAPackage("..dto..").should().beAnnotatedWith(Entity.class);

  @ArchTest
  static final ArchRule dtos_should_not_depend_on_repositories =
      noClasses()
          .that()
          .resideInAPackage("..dto..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..repository..");

  // ========================================================================================
  // GDPR data isolation (ADR-0014)
  // ========================================================================================

  @ArchTest
  static final ArchRule entities_should_not_store_pii =
      noFields()
          .that()
          .areDeclaredInClassesThat()
          .areAnnotatedWith(Entity.class)
          .should()
          .haveNameMatching(".*(?i)(cprNumber|cvrNumber|personName|address|email|phone).*")
          .as(
              "Entities must not store PII directly (ADR-0014). "
                  + "Use person-registry UUID references instead.");

  // ========================================================================================
  // Cross-service isolation (ADR-0007)
  // ========================================================================================

  @ArchTest
  static final ArchRule no_dependency_on_other_service_repositories =
      noClasses()
          .that()
          .resideInAPackage("dk.ufst.opendebt.creditorservice..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "dk.ufst.opendebt.debtservice..repository..",
              "dk.ufst.opendebt.personregistry..repository..",
              "dk.ufst.opendebt.payment..repository..")
          .as(
              "Creditor-service must not access other services' repositories directly (ADR-0007). "
                  + "Use API clients instead.");
}
