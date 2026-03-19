package dk.ufst.opendebt.citizen.config;

import java.util.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import dk.ufst.opendebt.citizen.client.PersonRegistryClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom OIDC user service that extracts CPR from the ID token, resolves person_id via
 * person-registry, and enriches the authentication principal with ROLE_CITIZEN. CPR is NEVER logged
 * or stored -- only the resolved person_id UUID is retained.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CitizenOidcUserService extends OidcUserService {

  public static final String PERSON_ID_ATTRIBUTE = "citizen_person_id";

  private final PersonRegistryClient personRegistryClient;
  private final CitizenAuthProperties authProperties;

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(userRequest);

    String cprClaimName = authProperties.getCprClaimName();
    Object cprClaim = oidcUser.getIdToken().getClaim(cprClaimName);
    if (cprClaim == null) {
      log.error("CPR claim '{}' not found in ID token", cprClaimName);
      throw new OAuth2AuthenticationException(
          new OAuth2Error("missing_cpr", "Identity claim not found in token", null));
    }

    String cpr = cprClaim.toString();
    UUID personId;
    try {
      personId = personRegistryClient.lookupOrCreatePerson(cpr);
    } catch (Exception ex) {
      log.error("Failed to resolve person_id from person-registry: {}", ex.getMessage());
      throw new OAuth2AuthenticationException(
          new OAuth2Error("person_resolution_failed", "Could not resolve citizen identity", null));
    }

    log.info("Citizen authenticated, person_id={}", personId);

    Set<GrantedAuthority> authorities = new LinkedHashSet<>(oidcUser.getAuthorities());
    authorities.add(new SimpleGrantedAuthority("ROLE_CITIZEN"));

    return new CitizenOidcUser(authorities, oidcUser, personId);
  }
}
