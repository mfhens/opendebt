package dk.ufst.opendebt.common.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Unit tests for {@link AuthContext} JWT claim extraction.
 *
 * <p>Tests verify that the fromSecurityContext() method correctly extracts: - User ID from "sub"
 * claim - Person ID from "person_id" claim (for CITIZEN role) - Organization ID from
 * "organization_id" claim (for CREDITOR role) - Roles from Spring Security authorities -
 * Capabilities from "capabilities" claim array
 */
class AuthContextTest {

  @BeforeEach
  void setUp() {
    // Clear SecurityContext before each test
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should extract full context for caseworker with capabilities")
  void shouldExtractCaseworkerContext() {
    // Given
    String userId = "user123";
    Set<String> roles = Set.of("ROLE_CASEWORKER");
    List<String> capabilities = List.of("HANDLE_VIP_CASES", "HANDLE_PEP_CASES");

    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", userId)
            .claim("capabilities", capabilities)
            .build();

    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CASEWORKER"));
    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    // When
    AuthContext authContext = AuthContext.fromSecurityContext();

    // Then
    assertThat(authContext.getUserId()).isEqualTo(userId);
    assertThat(authContext.getRoles()).containsExactly("CASEWORKER");
    assertThat(authContext.getCapabilities())
        .containsExactlyInAnyOrder("HANDLE_VIP_CASES", "HANDLE_PEP_CASES");
    assertThat(authContext.getPersonId()).isNull();
    assertThat(authContext.getOrganizationId()).isNull();
  }

  @Test
  @DisplayName("Should extract person_id for citizen role")
  void shouldExtractCitizenContext() {
    // Given
    String userId = "citizen-user-456";
    UUID personId = UUID.randomUUID();
    Set<String> roles = Set.of("ROLE_CITIZEN");

    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", userId)
            .claim("person_id", personId.toString())
            .build();

    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"));
    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    // When
    AuthContext authContext = AuthContext.fromSecurityContext();

    // Then
    assertThat(authContext.getUserId()).isEqualTo(userId);
    assertThat(authContext.getRoles()).containsExactly("CITIZEN");
    assertThat(authContext.getPersonId()).isEqualTo(personId);
    assertThat(authContext.getOrganizationId()).isNull();
  }

  @Test
  @DisplayName("Should extract organization_id for creditor role")
  void shouldExtractCreditorContext() {
    // Given
    String userId = "creditor-user-789";
    UUID organizationId = UUID.randomUUID();
    Set<String> roles = Set.of("ROLE_CREDITOR");

    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", userId)
            .claim("organization_id", organizationId.toString())
            .build();

    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CREDITOR"));
    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    // When
    AuthContext authContext = AuthContext.fromSecurityContext();

    // Then
    assertThat(authContext.getUserId()).isEqualTo(userId);
    assertThat(authContext.getRoles()).containsExactly("CREDITOR");
    assertThat(authContext.getOrganizationId()).isEqualTo(organizationId);
    assertThat(authContext.getPersonId()).isNull();
  }

  @Test
  @DisplayName("Should identify supervisor role correctly")
  void shouldIdentifySupervisor() {
    // Given
    Jwt jwt =
        Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", "supervisor123").build();

    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPERVISOR"));
    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    // When
    AuthContext authContext = AuthContext.fromSecurityContext();

    // Then
    assertThat(authContext.isSupervisorOrAdmin()).isTrue();
    assertThat(authContext.isAdmin()).isFalse();
    assertThat(authContext.hasRole("SUPERVISOR")).isTrue();
  }

  @Test
  @DisplayName("Should identify admin role correctly")
  void shouldIdentifyAdmin() {
    // Given
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", "admin123").build();

    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    // When
    AuthContext authContext = AuthContext.fromSecurityContext();

    // Then
    assertThat(authContext.isAdmin()).isTrue();
    assertThat(authContext.isSupervisorOrAdmin()).isTrue();
    assertThat(authContext.hasRole("ADMIN")).isTrue();
  }

  @Test
  @DisplayName("Should handle missing capabilities gracefully")
  void shouldHandleMissingCapabilities() {
    // Given
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", "user").build();

    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CASEWORKER"));
    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    // When
    AuthContext authContext = AuthContext.fromSecurityContext();

    // Then
    assertThat(authContext.getCapabilities()).isEmpty();
    assertThat(authContext.hasCapability("HANDLE_VIP_CASES")).isFalse();
  }

  @Test
  @DisplayName("Should validate capability presence correctly")
  void shouldValidateCapability() {
    // Given
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", "user")
            .claim("capabilities", List.of("HANDLE_VIP_CASES"))
            .build();

    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CASEWORKER"));
    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    // When
    AuthContext authContext = AuthContext.fromSecurityContext();

    // Then
    assertThat(authContext.hasCapability("HANDLE_VIP_CASES")).isTrue();
    assertThat(authContext.hasCapability("HANDLE_PEP_CASES")).isFalse();
  }

  @Test
  @DisplayName("Should throw exception when no authentication in context")
  void shouldThrowWhenNoAuthentication() {
    // Given
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(null);
    SecurityContextHolder.setContext(securityContext);

    // When / Then
    assertThatThrownBy(AuthContext::fromSecurityContext)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No authentication found");
  }

  @Test
  @DisplayName("Should throw exception when authentication is not JWT")
  void shouldThrowWhenNotJwtAuthentication() {
    // Given
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn("user");

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    // When / Then
    assertThatThrownBy(AuthContext::fromSecurityContext)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Expected JwtAuthenticationToken");
  }
}
