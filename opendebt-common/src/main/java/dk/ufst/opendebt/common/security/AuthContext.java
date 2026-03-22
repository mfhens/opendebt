package dk.ufst.opendebt.common.security;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import lombok.Builder;
import lombok.Value;

/**
 * Extracted authentication context from JWT token. Provides structured access to user identity,
 * roles, and capabilities.
 *
 * <p>Per ADR-0005 (Keycloak Authentication), all endpoints validate JWT tokens containing roles,
 * person_id, organization_id, and capabilities.
 */
@Value
@Builder
public class AuthContext {

  String userId;
  UUID personId; // For CITIZEN role
  UUID organizationId; // For CREDITOR role
  Set<String> roles;
  Set<String> capabilities; // HANDLE_VIP_CASES, HANDLE_PEP_CASES

  /** Extract AuthContext from Spring Security context. */
  public static AuthContext fromSecurityContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new IllegalStateException("No authentication found in security context");
    }

    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      throw new IllegalStateException(
          "Expected JwtAuthenticationToken but got " + authentication.getClass().getSimpleName());
    }

    Jwt jwt = jwtAuth.getToken();

    return AuthContext.builder()
        .userId(jwt.getClaimAsString("sub"))
        .personId(parseUuidClaim(jwt, "person_id"))
        .organizationId(parseUuidClaim(jwt, "organization_id"))
        .roles(
            authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                .collect(Collectors.toSet()))
        .capabilities(parseCapabilities(jwt))
        .build();
  }

  private static UUID parseUuidClaim(Jwt jwt, String claimName) {
    String value = jwt.getClaimAsString(claimName);
    return value != null ? UUID.fromString(value) : null;
  }

  @SuppressWarnings("unchecked")
  private static Set<String> parseCapabilities(Jwt jwt) {
    Object capabilitiesClaim = jwt.getClaim("capabilities");
    if (capabilitiesClaim instanceof java.util.List) {
      return ((java.util.List<String>) capabilitiesClaim).stream().collect(Collectors.toSet());
    }
    return Set.of();
  }

  public boolean hasRole(String role) {
    return roles.contains(role);
  }

  public boolean hasCapability(String capability) {
    return capabilities.contains(capability);
  }

  public boolean isSupervisorOrAdmin() {
    return hasRole("SUPERVISOR") || hasRole("ADMIN");
  }

  public boolean isAdmin() {
    return hasRole("ADMIN");
  }
}
