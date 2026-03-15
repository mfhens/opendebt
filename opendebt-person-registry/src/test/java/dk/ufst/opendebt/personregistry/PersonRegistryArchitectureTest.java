package dk.ufst.opendebt.personregistry;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import dk.ufst.opendebt.common.test.SharedArchRules;

@AnalyzeClasses(
    packages = "dk.ufst.opendebt.personregistry",
    importOptions = ImportOption.DoNotIncludeTests.class)
class PersonRegistryArchitectureTest {

  // PII rule intentionally excluded — person-registry IS the GDPR vault (ADR-0014).
  // It is the only service permitted to store encrypted PII fields.

  @ArchTest
  static final ArchRule no_cross_service_db_access =
      SharedArchRules.noAccessToOtherServiceRepositories("personregistry");
}
