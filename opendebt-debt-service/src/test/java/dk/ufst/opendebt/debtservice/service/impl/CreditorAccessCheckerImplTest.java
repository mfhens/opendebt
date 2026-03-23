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

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.security.AuthContext;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

/**
 * Unit tests for {@link CreditorAccessCheckerImpl}.
 *
 * <p>Tests verify authorization rules from petition048:
 *
 * <ul>
 *   <li>Rule 3.1: Creditors can only access claims where creditor_org_id matches their
 *       organization_id
 *   <li>Rule 3.2: Creditors can submit, modify, or withdraw their own claims
 *   <li>Rule 6.1: Admins have unrestricted access
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CreditorAccessCheckerImplTest {

  @Mock private DebtRepository debtRepository;
  @Mock private ClsAuditClient clsAuditClient;

  @InjectMocks private CreditorAccessCheckerImpl creditorAccessChecker;

  private UUID claimId;
  private UUID creditorOrgId;
  private UUID debtorPersonId;
  private DebtEntity claimEntity;

  @BeforeEach
  void setUp() {
    claimId = UUID.randomUUID();
    creditorOrgId = UUID.randomUUID();
    debtorPersonId = UUID.randomUUID();

    claimEntity =
        DebtEntity.builder()
            .id(claimId)
            .debtorPersonId(debtorPersonId)
            .creditorOrgId(creditorOrgId)
            .debtTypeCode("MUNICIPALFEE")
            .principalAmount(BigDecimal.valueOf(2000))
            .dueDate(LocalDate.now().minusMonths(1))
            .build();
  }

  @Test
  @DisplayName("Rule 6.1: Admin should have unrestricted access to all claims")
  void adminShouldHaveUnrestrictedAccess() {
    // Given
    AuthContext adminContext =
        AuthContext.builder().userId("admin1").roles(Set.of("ADMIN")).build();

    // When
    boolean hasAccess = creditorAccessChecker.canAccessClaim(claimId, adminContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(debtRepository, never()).findById(any()); // No DB query needed
  }

  @Test
  @DisplayName("Supervisor should have access to claims")
  void supervisorShouldHaveAccess() {
    // Given
    AuthContext supervisorContext =
        AuthContext.builder().userId("supervisor1").roles(Set.of("SUPERVISOR")).build();

    // When
    boolean hasAccess = creditorAccessChecker.canAccessClaim(claimId, supervisorContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(debtRepository, never()).findById(any()); // No DB query needed
  }

  @Test
  @DisplayName("Caseworker should have access to claims (via case context)")
  void caseworkerShouldHaveAccess() {
    // Given
    AuthContext caseworkerContext =
        AuthContext.builder().userId("caseworker1").roles(Set.of("CASEWORKER")).build();

    // When
    boolean hasAccess = creditorAccessChecker.canAccessClaim(claimId, caseworkerContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(debtRepository, never()).findById(any()); // No DB query needed
  }

  @Test
  @DisplayName("Rule 3.1: Creditor should access their own claim")
  void creditorShouldAccessOwnClaim() {
    // Given
    AuthContext creditorContext =
        AuthContext.builder()
            .userId("creditor1")
            .organizationId(creditorOrgId)
            .roles(Set.of("CREDITOR"))
            .build();

    when(debtRepository.findById(claimId)).thenReturn(Optional.of(claimEntity));

    // When
    boolean hasAccess = creditorAccessChecker.canAccessClaim(claimId, creditorContext);

    // Then
    assertThat(hasAccess).isTrue();
    verify(debtRepository).findById(claimId);
  }

  @Test
  @DisplayName("Rule 3.1: Creditor should NOT access another organization's claim")
  void creditorShouldNotAccessOtherClaim() {
    // Given
    UUID otherOrgId = UUID.randomUUID();
    AuthContext creditorContext =
        AuthContext.builder()
            .userId("creditor2")
            .organizationId(otherOrgId)
            .roles(Set.of("CREDITOR"))
            .build();

    when(debtRepository.findById(claimId)).thenReturn(Optional.of(claimEntity));

    // When
    boolean hasAccess = creditorAccessChecker.canAccessClaim(claimId, creditorContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(debtRepository).findById(claimId);
    verify(clsAuditClient)
        .shipEvent(
            argThat(
                event ->
                    event.getResourceId().equals(claimId)
                        && "DENY".equals(event.getOperation())
                        && "claim".equals(event.getResourceType())
                        && Boolean.FALSE.equals(event.getNewValues().get("granted"))));
  }

  @Test
  @DisplayName("Creditor without organization_id claim should be denied")
  void creditorWithoutOrgIdShouldBeDenied() {
    // Given
    AuthContext creditorContext =
        AuthContext.builder()
            .userId("creditor3")
            .organizationId(null) // Missing organization_id
            .roles(Set.of("CREDITOR"))
            .build();

    // When
    boolean hasAccess = creditorAccessChecker.canAccessClaim(claimId, creditorContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(debtRepository, never()).findById(any()); // Fail-fast validation
  }

  @Test
  @DisplayName("Creditor should be denied when claim not found")
  void creditorShouldBeDeniedWhenClaimNotFound() {
    // Given
    AuthContext creditorContext =
        AuthContext.builder()
            .userId("creditor4")
            .organizationId(creditorOrgId)
            .roles(Set.of("CREDITOR"))
            .build();

    when(debtRepository.findById(claimId)).thenReturn(Optional.empty());

    // When
    boolean hasAccess = creditorAccessChecker.canAccessClaim(claimId, creditorContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(debtRepository).findById(claimId);
  }

  @Test
  @DisplayName("Citizen should NOT have direct claim access")
  void citizenShouldNotAccessClaim() {
    // Given
    AuthContext citizenContext =
        AuthContext.builder()
            .userId("citizen1")
            .personId(UUID.randomUUID())
            .roles(Set.of("CITIZEN"))
            .build();

    // When
    boolean hasAccess = creditorAccessChecker.canAccessClaim(claimId, citizenContext);

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
    boolean hasAccess = creditorAccessChecker.canAccessClaim(claimId, unknownContext);

    // Then
    assertThat(hasAccess).isFalse();
    verify(debtRepository, never()).findById(any());
  }
}
