package dk.ufst.opendebt.debtservice.service.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import dk.ufst.opendebt.common.security.AuthContext;
import dk.ufst.opendebt.common.security.CreditorAccessChecker;
import dk.ufst.opendebt.common.security.DebtorAccessChecker;
import dk.ufst.opendebt.debtservice.config.TestConfig;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class CrossServiceAuthorizationRevalidationIntegrationTest {

  @Autowired private DebtRepository debtRepository;
  @Autowired private DebtorAccessChecker debtorAccessChecker;
  @Autowired private CreditorAccessChecker creditorAccessChecker;

  private UUID bobPersonId;
  private UUID orgB;
  private UUID debtForAlice;
  private UUID debtForBob;

  @BeforeEach
  void setUp() {
    debtRepository.deleteAll();

    UUID alicePersonId = UUID.randomUUID();
    bobPersonId = UUID.randomUUID();
    UUID orgA = UUID.randomUUID();
    orgB = UUID.randomUUID();

    debtForAlice = createDebt(alicePersonId, orgA, "CROSS-A");
    debtForBob = createDebt(bobPersonId, orgB, "CROSS-B");
  }

  @Test
  @DisplayName(
      "ADR-0007: Debt service re-validates citizen ownership and denies spoofed upstream context")
  void debtServiceRevalidatesCitizenOwnershipAgainstStoredDebt() {
    // Simulate downstream call context where upstream may have pre-filtered incorrectly.
    AuthContext spoofedCitizenContext =
        AuthContext.builder()
            .userId("citizen-b")
            .personId(bobPersonId)
            .roles(Set.of("CITIZEN"))
            .build();

    boolean allowed = debtorAccessChecker.canAccessDebt(debtForAlice, spoofedCitizenContext);

    assertThat(allowed).isFalse();
  }

  @Test
  @DisplayName("ADR-0007: Debt service re-validates creditor organization independently")
  void debtServiceRevalidatesCreditorOrganizationAgainstStoredDebt() {
    AuthContext wrongCreditorContext =
        AuthContext.builder()
            .userId("creditor-b")
            .organizationId(orgB)
            .roles(Set.of("CREDITOR"))
            .build();

    boolean allowed = creditorAccessChecker.canAccessClaim(debtForAlice, wrongCreditorContext);

    assertThat(allowed).isFalse();
  }

  @Test
  @DisplayName("Admin override still works after cross-service re-validation hardening")
  void adminOverrideRemainsAllowed() {
    AuthContext adminContext =
        AuthContext.builder().userId("admin-1").roles(Set.of("ADMIN")).build();

    boolean debtAllowed = debtorAccessChecker.canAccessDebt(debtForAlice, adminContext);
    boolean claimAllowed = creditorAccessChecker.canAccessClaim(debtForBob, adminContext);

    assertThat(debtAllowed).isTrue();
    assertThat(claimAllowed).isTrue();
  }

  private UUID createDebt(UUID debtorPersonId, UUID creditorOrgId, String externalReference) {
    DebtEntity debt =
        DebtEntity.builder()
            .debtorPersonId(debtorPersonId)
            .creditorOrgId(creditorOrgId)
            .debtTypeCode("INCOMETAX")
            .principalAmount(BigDecimal.valueOf(1500))
            .interestAmount(BigDecimal.ZERO)
            .feesAmount(BigDecimal.ZERO)
            .outstandingBalance(BigDecimal.valueOf(1500))
            .dueDate(LocalDate.now().minusDays(30))
            .externalReference(externalReference)
            .status(DebtEntity.DebtStatus.ACTIVE)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .build();
    return debtRepository.save(debt).getId();
  }
}
