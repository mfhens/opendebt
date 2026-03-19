package dk.ufst.opendebt.citizen.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import dk.ufst.opendebt.citizen.client.PersonRegistryClient;

@ExtendWith(MockitoExtension.class)
class CitizenOidcUserServiceTest {

  @Mock private PersonRegistryClient personRegistryClient;

  private CitizenAuthProperties authProperties;

  @BeforeEach
  void setUp() {
    authProperties = new CitizenAuthProperties();
    authProperties.setCprClaimName("cpr_number");
  }

  @Test
  void personRegistryClient_resolvesPersonId() {
    UUID expectedPersonId = UUID.randomUUID();
    when(personRegistryClient.lookupOrCreatePerson("0101011234")).thenReturn(expectedPersonId);

    UUID result = personRegistryClient.lookupOrCreatePerson("0101011234");
    assertThat(result).isEqualTo(expectedPersonId);
  }

  @Test
  void personRegistryClient_throwsOnFailure() {
    when(personRegistryClient.lookupOrCreatePerson(anyString()))
        .thenThrow(new RuntimeException("Person registry unavailable"));

    assertThatThrownBy(() -> personRegistryClient.lookupOrCreatePerson("0101011234"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Person registry unavailable");
  }

  @Test
  void citizenOidcUser_carriesPersonId() {
    UUID personId = UUID.randomUUID();
    OidcUser mockOidcUser = createMockOidcUser(Map.of("sub", "test-user"));

    CitizenOidcUser citizenUser =
        new CitizenOidcUser(mockOidcUser.getAuthorities(), mockOidcUser, personId);

    assertThat(citizenUser.getPersonId()).isEqualTo(personId);
    assertThat(citizenUser.getName()).isEqualTo(mockOidcUser.getName());
  }

  @Test
  void citizenOidcUser_delegatesGetters() {
    UUID personId = UUID.randomUUID();
    OidcUser mockOidcUser = createMockOidcUser(Map.of("sub", "test-user"));

    CitizenOidcUser citizenUser =
        new CitizenOidcUser(mockOidcUser.getAuthorities(), mockOidcUser, personId);

    assertThat(citizenUser.getIdToken()).isEqualTo(mockOidcUser.getIdToken());
    assertThat(citizenUser.getClaims()).isEqualTo(mockOidcUser.getClaims());
    assertThat(citizenUser.getAttributes()).isEqualTo(mockOidcUser.getAttributes());
    assertThat(citizenUser.getUserInfo()).isNull();
  }

  @Test
  void authProperties_defaultCprClaimName() {
    CitizenAuthProperties defaults = new CitizenAuthProperties();
    assertThat(defaults.getCprClaimName()).isEqualTo("dk:gov:saml:attribute:CprNumberIdentifier");
  }

  @Test
  void authProperties_configurableCprClaimName() {
    authProperties.setCprClaimName("custom_claim");
    assertThat(authProperties.getCprClaimName()).isEqualTo("custom_claim");
  }

  private OidcUser createMockOidcUser(Map<String, Object> claims) {
    java.util.Map<String, Object> allClaims = new java.util.HashMap<>(claims);
    allClaims.putIfAbsent("sub", "test-subject");
    allClaims.putIfAbsent("iss", "http://localhost:8080/realms/test");
    allClaims.putIfAbsent("aud", java.util.List.of("opendebt-citizen"));

    OidcIdToken idToken =
        new OidcIdToken("token-value", Instant.now(), Instant.now().plusSeconds(300), allClaims);

    return new DefaultOidcUser(
        java.util.List.of(
            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),
        idToken);
  }
}
