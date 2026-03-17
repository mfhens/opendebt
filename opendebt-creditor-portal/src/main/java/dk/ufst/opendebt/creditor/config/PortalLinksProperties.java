package dk.ufst.opendebt.creditor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configurable external link URLs for the creditor portal navigation. All URLs are set via
 * application properties under {@code opendebt.portal.links.*} and must never be hardcoded in
 * templates.
 */
@Data
@Component
@ConfigurationProperties(prefix = "opendebt.portal.links")
public class PortalLinksProperties {

  /** URL for the agreement material page (Aftalemateriale). */
  private String agreementMaterial = "https://gaeldst.dk/fordringshaver/individuelle-aftaler";

  /** URL for the creditor support contact page (Kontakt). */
  private String contact = "https://www.gaeldst.dk/fordringshaver/fordringshaversupport/";

  /** URL for the portal guides page (Guides). */
  private String guides =
      "https://www.gaeldst.dk/fordringshaver/find-vejledninger/guides-til-fordringshaverportalen/";
}
