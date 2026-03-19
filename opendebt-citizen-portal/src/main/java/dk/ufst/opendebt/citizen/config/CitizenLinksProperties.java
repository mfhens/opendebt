package dk.ufst.opendebt.citizen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "opendebt.citizen.external-links")
public class CitizenLinksProperties {

  private String mitGaeldsoverblik = "https://mitgaeldsoverblik.gaeldst.dk/";
  private String paymentInfo = "https://gaeldst.dk/borger/saadan-betaler-du-din-gaeld";
  private String interestRates = "https://gaeldst.dk/borger/betal-min-gaeld/renter-og-gebyrer";
  private String paymentDifficulties =
      "https://gaeldst.dk/borger/hvis-du-ikke-kan-betale-din-gaeld";
  private String debtCounselling =
      "https://gaeldst.dk/borger/hvis-du-ikke-kan-betale-din-gaeld/brug-for-raadgivning-om-din-gaeld";
  private String creditorList =
      "https://gaeldst.dk/borger/om-gaeld-til-inddrivelse/se-hvem-vi-inddriver-gaeld-for";
  private String debtErrors =
      "https://gaeldst.dk/borger/om-gaeld-til-inddrivelse/hvis-der-er-fejl-i-din-gaeld";
  private String phoneNumber = "70 15 73 04";
  private String phoneInternational = "+45 70 15 73 04";
}
