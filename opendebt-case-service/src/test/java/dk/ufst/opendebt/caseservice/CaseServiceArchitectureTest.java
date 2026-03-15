package dk.ufst.opendebt.caseservice;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import dk.ufst.opendebt.common.test.SharedArchRules;

@AnalyzeClasses(
    packages = "dk.ufst.opendebt.caseservice",
    importOptions = ImportOption.DoNotIncludeTests.class)
class CaseServiceArchitectureTest {

  @ArchTest
  static final ArchRule entities_must_not_store_pii = SharedArchRules.ENTITIES_MUST_NOT_STORE_PII;

  @ArchTest
  static final ArchRule no_cross_service_db_access =
      SharedArchRules.noAccessToOtherServiceRepositories("caseservice");
}
