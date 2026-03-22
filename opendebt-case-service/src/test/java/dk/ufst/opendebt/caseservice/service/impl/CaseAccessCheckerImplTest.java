package dk.ufst.opendebt.caseservice.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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

import dk.ufst.opendebt.caseservice.entity.CaseEntity;
import dk.ufst.opendebt.caseservice.entity.CaseSensitivity;
import dk.ufst.opendebt.caseservice.repository.CaseRepository;
import dk.ufst.opendebt.common.security.AuthContext;

/**
 * Unit tests for {@link CaseAccessCheckerImpl}.
 *
 * <p>Tests verify authorization rules from petition048:
 *
 * <ul>
 *   <li>Rule 1.1: Caseworkers can only access assigned cases
 *   <li>Rule 2.1/2.2: Supervisors have full visibility
 *   <li>Rule 6.1: Admins have unrestricted access
 *   <li>Rule 3.3/4.3: Creditors and citizens cannot directly access cases
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CaseAccessCheckerImplTest {

  @Mock private CaseRepository caseRepository;

  @InjectMocks private CaseAccessCheckerImpl caseAccessChecker;

  private UUID caseId;
  private CaseEntity caseEntity;

  @BeforeEach
  void setUp() {
    caseId = UUID.randomUUID();
    caseEntity = new CaseEntity();
    caseEntity.setId(caseId);
    caseEntity.setPrimaryCaseworkerId("caseworker123");
  }

  @Test
  @DisplayName("Rule 6.1: Admin should have unrestricted access to all cases")
  void adminShouldHaveUnrestrictedAccess() {
    // Given
    AuthContext adminContext =
        AuthContext.builder().userId("admin1").roles(Set.of("ADMIN")).build();

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, adminContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(caseRepository, never()).findById(any()); // No DB query needed
  }

  @Test
  @DisplayName("Rule 2.1: Supervisor should have full visibility to all cases")
  void supervisorShouldHaveFullVisibility() {
    // Given
    AuthContext supervisorContext =
        AuthContext.builder().userId("supervisor1").roles(Set.of("SUPERVISOR")).build();

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, supervisorContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(caseRepository, never()).findById(any()); // No DB query needed
  }

  @Test
  @DisplayName("Rule 1.1: Caseworker should access assigned case")
  void caseworkerShouldAccessAssignedCase() {
    // Given
    String caseworkerId = "caseworker123";
    AuthContext caseworkerContext =
        AuthContext.builder().userId(caseworkerId).roles(Set.of("CASEWORKER")).build();

    when(caseRepository.findById(caseId)).thenReturn(Optional.of(caseEntity));

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, caseworkerContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(caseRepository).findById(caseId);
  }

  @Test
  @DisplayName("Rule 1.1: Caseworker should NOT access unassigned case")
  void caseworkerShouldNotAccessUnassignedCase() {
    // Given
    String caseworkerId = "otherCaseworker456";
    AuthContext caseworkerContext =
        AuthContext.builder().userId(caseworkerId).roles(Set.of("CASEWORKER")).build();

    when(caseRepository.findById(caseId)).thenReturn(Optional.of(caseEntity));

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, caseworkerContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(caseRepository).findById(caseId);
  }

  @Test
  @DisplayName("Caseworker should be denied when case not found")
  void caseworkerShouldBeDeniedWhenCaseNotFound() {
    // Given
    AuthContext caseworkerContext =
        AuthContext.builder().userId("caseworker123").roles(Set.of("CASEWORKER")).build();

    when(caseRepository.findById(caseId)).thenReturn(Optional.empty());

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, caseworkerContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(caseRepository).findById(caseId);
  }

  @Test
  @DisplayName("Rule 3.3: Creditor should NOT have direct case access")
  void creditorShouldNotAccessCase() {
    // Given
    AuthContext creditorContext =
        AuthContext.builder()
            .userId("creditor1")
            .organizationId(UUID.randomUUID())
            .roles(Set.of("CREDITOR"))
            .build();

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, creditorContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(caseRepository, never()).findById(any()); // No DB query needed (fail-fast)
  }

  @Test
  @DisplayName("Rule 4.3: Citizen should NOT have direct case access")
  void citizenShouldNotAccessCase() {
    // Given
    AuthContext citizenContext =
        AuthContext.builder()
            .userId("citizen1")
            .personId(UUID.randomUUID())
            .roles(Set.of("CITIZEN"))
            .build();

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, citizenContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(caseRepository, never()).findById(any()); // No DB query needed (fail-fast)
  }

  @Test
  @DisplayName("Unknown role should be denied access")
  void unknownRoleShouldBeDenied() {
    // Given
    AuthContext unknownContext =
        AuthContext.builder().userId("unknown1").roles(Set.of("UNKNOWN")).build();

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, unknownContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(caseRepository, never()).findById(any());
  }

  // ── Sensitivity Filtering Tests (W9-RBAC-02) ────────────────────────

  @Test
  @DisplayName("Rule 5.3: Caseworker should NOT access CONFIDENTIAL case")
  void caseworkerShouldNotAccessConfidentialCase() {
    // Given
    caseEntity.setSensitivity(CaseSensitivity.CONFIDENTIAL);
    AuthContext caseworkerContext =
        AuthContext.builder()
            .userId("caseworker123")
            .roles(Set.of("CASEWORKER"))
            .capabilities(Set.of("HANDLE_VIP_CASES", "HANDLE_PEP_CASES"))
            .build();

    when(caseRepository.findById(caseId)).thenReturn(Optional.of(caseEntity));

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, caseworkerContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(caseRepository).findById(caseId);
  }

  @Test
  @DisplayName("Rule 5.2: Caseworker WITH HANDLE_VIP_CASES should access VIP case")
  void caseworkerWithVipCapabilityShouldAccessVipCase() {
    // Given
    caseEntity.setSensitivity(CaseSensitivity.VIP);
    AuthContext caseworkerContext =
        AuthContext.builder()
            .userId("caseworker123")
            .roles(Set.of("CASEWORKER"))
            .capabilities(Set.of("HANDLE_VIP_CASES"))
            .build();

    when(caseRepository.findById(caseId)).thenReturn(Optional.of(caseEntity));

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, caseworkerContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(caseRepository).findById(caseId);
  }

  @Test
  @DisplayName("Rule 5.2: Caseworker WITHOUT HANDLE_VIP_CASES should NOT access VIP case")
  void caseworkerWithoutVipCapabilityShouldNotAccessVipCase() {
    // Given
    caseEntity.setSensitivity(CaseSensitivity.VIP);
    AuthContext caseworkerContext =
        AuthContext.builder()
            .userId("caseworker123")
            .roles(Set.of("CASEWORKER"))
            .capabilities(Set.of()) // No VIP capability
            .build();

    when(caseRepository.findById(caseId)).thenReturn(Optional.of(caseEntity));

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, caseworkerContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(caseRepository).findById(caseId);
  }

  @Test
  @DisplayName("Rule 5.2: Caseworker WITH HANDLE_PEP_CASES should access PEP case")
  void caseworkerWithPepCapabilityShouldAccessPepCase() {
    // Given
    caseEntity.setSensitivity(CaseSensitivity.PEP);
    AuthContext caseworkerContext =
        AuthContext.builder()
            .userId("caseworker123")
            .roles(Set.of("CASEWORKER"))
            .capabilities(Set.of("HANDLE_PEP_CASES"))
            .build();

    when(caseRepository.findById(caseId)).thenReturn(Optional.of(caseEntity));

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, caseworkerContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(caseRepository).findById(caseId);
  }

  @Test
  @DisplayName("Rule 5.2: Caseworker WITHOUT HANDLE_PEP_CASES should NOT access PEP case")
  void caseworkerWithoutPepCapabilityShouldNotAccessPepCase() {
    // Given
    caseEntity.setSensitivity(CaseSensitivity.PEP);
    AuthContext caseworkerContext =
        AuthContext.builder()
            .userId("caseworker123")
            .roles(Set.of("CASEWORKER"))
            .capabilities(Set.of()) // No PEP capability
            .build();

    when(caseRepository.findById(caseId)).thenReturn(Optional.of(caseEntity));

    // When
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, caseworkerContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(caseRepository).findById(caseId);
  }

  @Test
  @DisplayName("Supervisor should access CONFIDENTIAL case")
  void supervisorShouldAccessConfidentialCase() {
    // Given
    caseEntity.setSensitivity(CaseSensitivity.CONFIDENTIAL);
    AuthContext supervisorContext =
        AuthContext.builder().userId("supervisor1").roles(Set.of("SUPERVISOR")).build();

    // When - supervisor bypass doesn't query repository
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, supervisorContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(caseRepository, never()).findById(any()); // Supervisor bypass
  }

  @Test
  @DisplayName("Admin should access CONFIDENTIAL case")
  void adminShouldAccessConfidentialCase() {
    // Given
    caseEntity.setSensitivity(CaseSensitivity.CONFIDENTIAL);
    AuthContext adminContext =
        AuthContext.builder().userId("admin1").roles(Set.of("ADMIN")).build();

    // When - admin bypass doesn't query repository
    boolean hasAccess = caseAccessChecker.canAccessCase(caseId, adminContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(caseRepository, never()).findById(any()); // Admin bypass
  }
}
