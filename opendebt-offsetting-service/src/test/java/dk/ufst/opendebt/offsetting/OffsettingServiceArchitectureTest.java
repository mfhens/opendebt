package dk.ufst.opendebt.offsetting;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import dk.ufst.opendebt.common.test.SharedArchRules;

@AnalyzeClasses(
    packages = "dk.ufst.opendebt.offsetting",
    importOptions = ImportOption.DoNotIncludeTests.class)
class OffsettingServiceArchitectureTest {

  @ArchTest
  static final ArchRule no_cross_service_db_access =
      SharedArchRules.noAccessToOtherServiceRepositories("offsetting");
}
