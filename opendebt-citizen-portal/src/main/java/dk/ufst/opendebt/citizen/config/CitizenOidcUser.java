package dk.ufst.opendebt.citizen.config;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Custom OidcUser implementation that carries the resolved person_id UUID. This avoids storing CPR
 * anywhere -- only the technical person_id is accessible.
 */
public class CitizenOidcUser implements OidcUser {

  private final Collection<? extends GrantedAuthority> authorities;
  private final OidcUser delegate;
  private final UUID personId;

  public CitizenOidcUser(
      Collection<? extends GrantedAuthority> authorities, OidcUser delegate, UUID personId) {
    this.authorities = authorities;
    this.delegate = delegate;
    this.personId = personId;
  }

  public UUID getPersonId() {
    return personId;
  }

  @Override
  public Map<String, Object> getClaims() {
    return delegate.getClaims();
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return delegate.getUserInfo();
  }

  @Override
  public OidcIdToken getIdToken() {
    return delegate.getIdToken();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return delegate.getAttributes();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getName() {
    return delegate.getName();
  }
}
