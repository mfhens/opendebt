package dk.ufst.opendebt.citizen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "opendebt.citizen.auth")
public class CitizenAuthProperties {

  /** The JWT/OIDC claim name that contains the citizen CPR number. */
  private String cprClaimName = "dk:gov:saml:attribute:CprNumberIdentifier";
}
