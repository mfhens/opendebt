package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import dk.ufst.opendebt.common.security.AuthContext;
import dk.ufst.opendebt.common.security.CreditorAccessChecker;
import dk.ufst.opendebt.common.security.DebtorAccessChecker;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition048Steps {

  @Autowired private DebtRepository debtRepository;
  @Autowired private DebtorAccessChecker debtorAccessChecker;
  @Autowired private CreditorAccessChecker creditorAccessChecker;

  private UUID personA;
  private UUID personB;
  private UUID orgA;
  private UUID orgB;
  private UUID debtOwnedByPersonA;
  private UUID debtOwnedByPersonB;
  private UUID claimOwnedByOrgA;
  private UUID claimOwnedByOrgB;

  private AuthContext authContext;
  private String upstreamClaimedPerson;
  private Boolean lastAccessDecision;

  @Before
  public void resetScenarioState() {
    debtRepository.deleteAll();
    personA = UUID.randomUUID();
    personB = UUID.randomUUID();
    orgA = UUID.randomUUID();
    orgB = UUID.randomUUID();
    upstreamClaimedPerson = null;
    authContext = null;
    lastAccessDecision = null;
  }

  @Given("RBAC test debts are seeded for multiple debtors and creditor organizations")
  public void seedRbacDebtFixtures() {
    debtOwnedByPersonA = createDebt(personA, orgA, "RBAC-A-1");
    debtOwnedByPersonB = createDebt(personB, orgB, "RBAC-B-1");

    // Claims are represented by debt records in this service.
    claimOwnedByOrgA = debtOwnedByPersonA;
    claimOwnedByOrgB = debtOwnedByPersonB;
  }

  @Given("a citizen auth context for debtor person A")
  public void citizenAuthForPersonA() {
    authContext =
        AuthContext.builder()
            .userId("citizen-a")
            .personId(personA)
            .roles(Set.of("CITIZEN"))
            .build();
  }

  @Given("a citizen auth context for debtor person B")
  public void citizenAuthForPersonB() {
    authContext =
        AuthContext.builder()
            .userId("citizen-b")
            .personId(personB)
            .roles(Set.of("CITIZEN"))
            .build();
  }

  @Given("a creditor auth context for organization A")
  public void creditorAuthForOrgA() {
    authContext =
        AuthContext.builder()
            .userId("creditor-a")
            .organizationId(orgA)
            .roles(Set.of("CREDITOR"))
            .build();
  }

  @Given("an admin auth context")
  public void adminAuthContext() {
    authContext = AuthContext.builder().userId("admin-1").roles(Set.of("ADMIN")).build();
  }

  @Given("upstream case-service context claims debtor person A")
  public void upstreamClaimsPersonA() {
    upstreamClaimedPerson = personA.toString();
  }

  @When("the citizen requests access to a debt owned by person A")
  public void citizenRequestsDebtOwnedByPersonA() {
    lastAccessDecision = debtorAccessChecker.canAccessDebt(debtOwnedByPersonA, authContext);
  }

  @When("the citizen requests access to a debt owned by person B")
  public void citizenRequestsDebtOwnedByPersonB() {
    lastAccessDecision = debtorAccessChecker.canAccessDebt(debtOwnedByPersonB, authContext);
  }

  @When("the creditor requests access to a claim owned by organization A")
  public void creditorRequestsClaimOwnedByOrgA() {
    lastAccessDecision = creditorAccessChecker.canAccessClaim(claimOwnedByOrgA, authContext);
  }

  @When("the creditor requests access to a claim owned by organization B")
  public void creditorRequestsClaimOwnedByOrgB() {
    lastAccessDecision = creditorAccessChecker.canAccessClaim(claimOwnedByOrgB, authContext);
  }

  @When("debt-service re-validates access to a debt owned by person A")
  public void debtServiceRevalidatesDebtAccessForPersonA() {
    // Upstream claims are intentionally ignored; debt-service re-validates from its own data.
    assertThat(upstreamClaimedPerson).isEqualTo(personA.toString());
    lastAccessDecision = debtorAccessChecker.canAccessDebt(debtOwnedByPersonA, authContext);
  }

  @When("the admin requests access to a debt owned by person B")
  public void adminRequestsDebtOwnedByPersonB() {
    lastAccessDecision = debtorAccessChecker.canAccessDebt(debtOwnedByPersonB, authContext);
  }

  @When("the admin requests access to a claim owned by organization B")
  public void adminRequestsClaimOwnedByOrgB() {
    lastAccessDecision = creditorAccessChecker.canAccessClaim(claimOwnedByOrgB, authContext);
  }

  @Then("debt-service should grant debt access")
  public void shouldGrantDebtAccess() {
    assertThat(lastAccessDecision).isTrue();
  }

  @Then("debt-service should deny debt access")
  public void shouldDenyDebtAccess() {
    assertThat(lastAccessDecision).isFalse();
  }

  @Then("debt-service should grant claim access")
  public void shouldGrantClaimAccess() {
    assertThat(lastAccessDecision).isTrue();
  }

  @Then("debt-service should deny claim access")
  public void shouldDenyClaimAccess() {
    assertThat(lastAccessDecision).isFalse();
  }

  private UUID createDebt(UUID debtorPersonId, UUID creditorOrgId, String reference) {
    DebtEntity debt =
        DebtEntity.builder()
            .debtorPersonId(debtorPersonId)
            .creditorOrgId(creditorOrgId)
            .debtTypeCode("MUNICIPALFEE")
            .principalAmount(BigDecimal.valueOf(1000))
            .interestAmount(BigDecimal.ZERO)
            .feesAmount(BigDecimal.ZERO)
            .outstandingBalance(BigDecimal.valueOf(1000))
            .dueDate(LocalDate.now().minusDays(7))
            .externalReference(reference)
            .status(DebtEntity.DebtStatus.ACTIVE)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .build();
    return debtRepository.save(debt).getId();
  }
}
