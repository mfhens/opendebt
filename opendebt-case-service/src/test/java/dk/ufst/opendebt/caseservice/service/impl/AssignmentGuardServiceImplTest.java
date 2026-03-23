package dk.ufst.opendebt.caseservice.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import dk.ufst.opendebt.caseservice.entity.CaseSensitivity;
import dk.ufst.opendebt.caseservice.repository.CaseEventRepository;

/**
 * Unit tests for {@link AssignmentGuardServiceImpl}.
 *
 * <p>Tests verify capability-based assignment validation (Petition048 W9-RBAC-02):
 *
 * <ul>
 *   <li>Rule 5.2: VIP cases require HANDLE_VIP_CASES capability
 *   <li>Rule 5.2: PEP cases require HANDLE_PEP_CASES capability
 *   <li>Rule 5.3: CONFIDENTIAL cases cannot be assigned to caseworkers
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AssignmentGuardServiceImplTest {

  @Mock private CaseEventRepository caseEventRepository;

  @InjectMocks private AssignmentGuardServiceImpl assignmentGuardService;

  private UUID caseId;

  @BeforeEach
  void setUp() {
    caseId = UUID.randomUUID();
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Rule 5.2: Should allow assignment to VIP case with HANDLE_VIP_CASES capability")
  void shouldAllowVipAssignmentWithCapability() {
    // Given
    mockAuthContext("supervisor1", Set.of("HANDLE_VIP_CASES"));

    // When / Then - should not throw
    assertThatCode(
            () ->
                assignmentGuardService.validateAssignment(
                    caseId, "caseworker123", CaseSensitivity.VIP))
        .doesNotThrowAnyException();

    verify(caseEventRepository)
        .save(argThat(event -> event.getEventType().name().equals("CASEWORKER_ASSIGNED")));
  }

  @Test
  @DisplayName("Rule 5.2: Should reject VIP assignment without HANDLE_VIP_CASES capability")
  void shouldRejectVipAssignmentWithoutCapability() {
    // Given
    mockAuthContext("supervisor1", Set.of()); // No VIP capability

    // When / Then
    assertThatThrownBy(
            () ->
                assignmentGuardService.validateAssignment(
                    caseId, "caseworker123", CaseSensitivity.VIP))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("CASEWORKER_LACKS_VIP_PERMISSION");

    verify(caseEventRepository)
        .save(argThat(event -> event.getEventType().name().equals("ASSIGNMENT_DENIED")));
  }

  @Test
  @DisplayName("Rule 5.2: Should allow assignment to PEP case with HANDLE_PEP_CASES capability")
  void shouldAllowPepAssignmentWithCapability() {
    // Given
    mockAuthContext("supervisor1", Set.of("HANDLE_PEP_CASES"));

    // When / Then - should not throw
    assertThatCode(
            () ->
                assignmentGuardService.validateAssignment(
                    caseId, "caseworker123", CaseSensitivity.PEP))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Rule 5.2: Should reject PEP assignment without HANDLE_PEP_CASES capability")
  void shouldRejectPepAssignmentWithoutCapability() {
    // Given
    mockAuthContext("supervisor1", Set.of()); // No PEP capability

    // When / Then
    assertThatThrownBy(
            () ->
                assignmentGuardService.validateAssignment(
                    caseId, "caseworker123", CaseSensitivity.PEP))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("CASEWORKER_LACKS_PEP_PERMISSION");
  }

  @Test
  @DisplayName("Rule 5.3: Should reject CONFIDENTIAL assignment to any caseworker")
  void shouldRejectConfidentialAssignment() {
    // Given
    mockAuthContext("supervisor1", Set.of("HANDLE_VIP_CASES", "HANDLE_PEP_CASES"));

    // When / Then
    assertThatThrownBy(
            () ->
                assignmentGuardService.validateAssignment(
                    caseId, "caseworker123", CaseSensitivity.CONFIDENTIAL))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("CONFIDENTIAL cases cannot be assigned to caseworkers");
  }

  @Test
  @DisplayName("Should allow NORMAL case assignment without any capabilities")
  void shouldAllowNormalAssignment() {
    // Given
    mockAuthContext("supervisor1", Set.of()); // No capabilities

    // When / Then - should not throw
    assertThatCode(
            () ->
                assignmentGuardService.validateAssignment(
                    caseId, "caseworker123", CaseSensitivity.NORMAL))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should allow VIP assignment when caseworker has both VIP and PEP capabilities")
  void shouldAllowVipAssignmentWithMultipleCapabilities() {
    // Given
    mockAuthContext("supervisor1", Set.of("HANDLE_VIP_CASES", "HANDLE_PEP_CASES"));

    // When / Then - should not throw
    assertThatCode(
            () ->
                assignmentGuardService.validateAssignment(
                    caseId, "caseworker123", CaseSensitivity.VIP))
        .doesNotThrowAnyException();
  }

  /**
   * Helper method to mock authentication context with capabilities.
   *
   * @param userId User ID
   * @param capabilities Set of capability strings
   */
  private void mockAuthContext(String userId, Set<String> capabilities) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", userId)
            .claim("capabilities", capabilities.stream().toList())
            .build();

    Authentication authentication =
        new JwtAuthenticationToken(jwt, Set.of(new SimpleGrantedAuthority("ROLE_SUPERVISOR")));

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
  }
}
