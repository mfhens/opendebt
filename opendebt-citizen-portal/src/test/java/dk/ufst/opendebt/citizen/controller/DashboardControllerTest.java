package dk.ufst.opendebt.citizen.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

class DashboardControllerTest {

  private final DashboardController controller = new DashboardController();

  @Test
  void dashboard_withUser_addsPersonIdToModel() {
    UUID personId = UUID.randomUUID();
    var mockUser = createCitizenUser(personId);
    Model model = new ExtendedModelMap();

    String view = controller.dashboard(mockUser, model);

    assertThat(view).isEqualTo("dashboard");
    assertThat(model.getAttribute("personId")).isEqualTo(personId);
  }

  @Test
  void dashboard_withNullUser_noPersonIdInModel() {
    Model model = new ExtendedModelMap();

    String view = controller.dashboard(null, model);

    assertThat(view).isEqualTo("dashboard");
    assertThat(model.getAttribute("personId")).isNull();
  }

  private dk.ufst.opendebt.citizen.config.CitizenOidcUser createCitizenUser(UUID personId) {
    java.time.Instant now = java.time.Instant.now();
    var idToken =
        new org.springframework.security.oauth2.core.oidc.OidcIdToken(
            "token",
            now,
            now.plusSeconds(300),
            java.util.Map.of(
                "sub", "test-user",
                "iss", "http://localhost:8080/realms/test",
                "aud", java.util.List.of("opendebt-citizen")));

    var oidcUser =
        new org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(
            java.util.List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_CITIZEN")),
            idToken);

    return new dk.ufst.opendebt.citizen.config.CitizenOidcUser(
        oidcUser.getAuthorities(), oidcUser, personId);
  }
}
