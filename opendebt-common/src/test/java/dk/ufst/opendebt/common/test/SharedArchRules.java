package dk.ufst.opendebt.common.test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import jakarta.persistence.Entity;

import com.tngtech.archunit.lang.ArchRule;

/**
 * Shared ArchUnit rules enforcing cross-cutting architectural constraints.
 *
 * <p>Services use these by referencing the static fields in their own ArchUnit test classes. This
 * avoids duplicating rule definitions across 12 services.
 *
 * <p>Usage in a service test:
 *
 * <pre>{@code
 * @ArchTest static final ArchRule pii = SharedArchRules.ENTITIES_MUST_NOT_STORE_PII;
 * @ArchTest static final ArchRule db  = SharedArchRules.noAccessToOtherServiceRepositories("myservice");
 * }</pre>
 */
public final class SharedArchRules {

  private SharedArchRules() {}

  private static final String[] ALL_SERVICE_REPO_PACKAGES = {
    "dk.ufst.opendebt.creditorservice..repository..",
    "dk.ufst.opendebt.debtservice..repository..",
    "dk.ufst.opendebt.personregistry..repository..",
    "dk.ufst.opendebt.payment..repository..",
    "dk.ufst.opendebt.caseservice..repository..",
    "dk.ufst.opendebt.gateway..repository..",
    "dk.ufst.opendebt.letter..repository..",
    "dk.ufst.opendebt.offsetting..repository..",
    "dk.ufst.opendebt.wagegarnishment..repository..",
    "dk.ufst.opendebt.citizen..repository..",
    "dk.ufst.opendebt.rules..repository..",
    "dk.ufst.opendebt.creditor..repository..",
  };

  /**
   * ADR-0014: Entities must not store PII directly. Field names matching CPR, CVR, name, address,
   * email, or phone patterns are banned. Use person-registry UUID references instead.
   */
  public static final ArchRule ENTITIES_MUST_NOT_STORE_PII =
      noFields()
          .that()
          .areDeclaredInClassesThat()
          .areAnnotatedWith(Entity.class)
          .should()
          .haveNameMatching(
              ".*(?i)(cprNumber|cvrNumber|personName|fullName|address|email|phone|phoneNumber).*")
          .as(
              "Entities must not store PII directly (ADR-0014). "
                  + "Use person-registry UUID references instead.");

  /**
   * ADR-0007: A service must not depend on another service's repository classes. Returns a rule
   * scoped to the given service's base package that forbids importing repository classes from any
   * other service.
   *
   * @param serviceBasePackage the calling service's base package suffix, e.g. "creditorservice",
   *     "debtservice", "payment"
   */
  public static ArchRule noAccessToOtherServiceRepositories(String serviceBasePackage) {
    String[] forbidden =
        java.util.Arrays.stream(ALL_SERVICE_REPO_PACKAGES)
            .filter(pkg -> !pkg.startsWith("dk.ufst.opendebt." + serviceBasePackage))
            .toArray(String[]::new);

    return noClasses()
        .that()
        .resideInAPackage("dk.ufst.opendebt." + serviceBasePackage + "..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(forbidden)
        .as(
            serviceBasePackage
                + " must not access other services' repositories directly (ADR-0007). "
                + "Use API clients instead.");
  }
}
