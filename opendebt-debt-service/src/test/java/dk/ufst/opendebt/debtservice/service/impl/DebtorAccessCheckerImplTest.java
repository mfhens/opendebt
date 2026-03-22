package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.common.security.AuthContext;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

/**
 * Unit tests for {@link DebtorAccessCheckerImpl}.
 *
 * <p>Tests verify authorization rules from petition048:
 *
 * <ul>
 *   <li>Rule 4.1: Citizens can only access debts where debtor_person_id matches their person_id
 *   <li>Rule 6.1: Admins have unrestricted access
 *   <li>Supervisors and caseworkers should access debts through case-service
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class DebtorAccessCheckerImplTest {

  @Mock private DebtRepository debtRepository;

  @InjectMocks private DebtorAccessCheckerImpl debtorAccessChecker;

  private UUID debtId;
  private UUID citizenPersonId;
  private UUID creditorOrgId;
  private DebtEntity debtEntity;

  @BeforeEach
  void setUp() {
    debtId = UUID.randomUUID();
    citizenPersonId = UUID.randomUUID();
    creditorOrgId = UUID.randomUUID();

    debtEntity =
        DebtEntity.builder()
            .id(debtId)
            .debtorPersonId(citizenPersonId)
            .creditorOrgId(creditorOrgId)
            .debtTypeCode("INCOMETAX")
            .principalAmount(BigDecimal.valueOf(5000))
            .dueDate(LocalDate.now().minusMonths(3))
            .build();
  }

  @Test
  @DisplayName("Rule 6.1: Admin should have unrestricted access to all debts")
  void adminShouldHaveUnrestrictedAccess() {
    // Given
    AuthContext adminContext =
        AuthContext.builder().userId("admin1").roles(Set.of("ADMIN")).build();

    // When
    boolean hasAccess = debtorAccessChecker.canAccessDebt(debtId, adminContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(debtRepository, never()).findById(any()); // No DB query needed
  }

  @Test
  @DisplayName("Supervisor should have access to debts")
  void supervisorShouldHaveAccess() {
    // Given
    AuthContext supervisorContext =
        AuthContext.builder().userId("supervisor1").roles(Set.of("SUPERVISOR")).build();

    // When
    boolean hasAccess = debtorAccessChecker.canAccessDebt(debtId, supervisorContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(debtRepository, never()).findById(any()); // No DB query needed
  }

  @Test
  @DisplayName("Caseworker should have access to debts (via case context)")
  void caseworkerShouldHaveAccess() {
    // Given
    AuthContext caseworkerContext =
        AuthContext.builder().userId("caseworker1").roles(Set.of("CASEWORKER")).build();

    // When
    boolean hasAccess = debtorAccessChecker.canAccessDebt(debtId, caseworkerContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(debtRepository, never()).findById(any()); // No DB query needed
  }

  @Test
  @DisplayName("Rule 4.1: Citizen should access their own debt")
  void citizenShouldAccessOwnDebt() {
    // Given
    AuthContext citizenContext =
        AuthContext.builder()
            .userId("citizen1")
            .personId(citizenPersonId)
            .roles(Set.of("CITIZEN"))
            .build();

    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debtEntity));

    // When
    boolean hasAccess = debtorAccessChecker.canAccessDebt(debtId, citizenContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(debtRepository).findById(debtId);
  }

  @Test
  @DisplayName("Rule 4.1: Citizen should NOT access another person's debt")
  void citizenShouldNotAccessOtherDebt() {
    // Given
    UUID otherPersonId = UUID.randomUUID();
    AuthContext citizenContext =
        AuthContext.builder()
            .userId("citizen2")
            .personId(otherPersonId)
            .roles(Set.of("CITIZEN"))
            .build();

    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debtEntity));

    // When
    boolean hasAccess = debtorAccessChecker.canAccessDebt(debtId, citizenContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(debtRepository).findById(debtId);
  }

  @Test
  @DisplayName("Citizen without person_id claim should be denied")
  void citizenWithoutPersonIdShouldBeDenied() {
    // Given
    AuthContext citizenContext =
        AuthContext.builder()
            .userId("citizen3")
            .personId(null) // Missing person_id
            .roles(Set.of("CITIZEN"))
            .build();

    // When
    boolean hasAccess = debtorAccessChecker.canAccessDebt(debtId, citizenContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(debtRepository, never()).findById(any()); // Fail-fast validation
  }

  @Test
  @DisplayName("Citizen should be denied when debt not found")
  void citizenShouldBeDeniedWhenDebtNotFound() {
    // Given
    AuthContext citizenContext =
        AuthContext.builder()
            .userId("citizen4")
            .personId(citizenPersonId)
            .roles(Set.of("CITIZEN"))
            .build();

    when(debtRepository.findById(debtId)).thenReturn(Optional.empty());

    // When
    boolean hasAccess = debtorAccessChecker.canAccessDebt(debtId, citizenContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(debtRepository).findById(debtId);
  }

  @Test
  @DisplayName("Creditor should NOT have direct debt access")
  void creditorShouldNotAccessDebt() {
    // Given
    AuthContext creditorContext =
        AuthContext.builder()
            .userId("creditor1")
            .organizationId(UUID.randomUUID())
            .roles(Set.of("CREDITOR"))
            .build();

    // When
    boolean hasAccess = debtorAccessChecker.canAccessDebt(debtId, creditorContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(debtRepository, never()).findById(any()); // Fail-fast
  }

  @Test
  @DisplayName("Unknown role should be denied access")
  void unknownRoleShouldBeDenied() {
    // Given
    AuthContext unknownContext =
        AuthContext.builder().userId("unknown1").roles(Set.of("UNKNOWN")).build();

    // When
    boolean hasAccess = debtorAccessChecker.canAccessDebt(debtId, unknownContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(debtRepository, never()).findById(any());
  }
}
