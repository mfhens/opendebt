package dk.ufst.opendebt.rules;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import dk.ufst.opendebt.common.test.SharedArchRules;

@AnalyzeClasses(
    packages = "dk.ufst.opendebt.rules",
    importOptions = ImportOption.DoNotIncludeTests.class)
class RulesEngineArchitectureTest {

  @ArchTest
  static final ArchRule no_cross_service_db_access =
      SharedArchRules.noAccessToOtherServiceRepositories("rules");
}
