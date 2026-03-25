package dk.ufst.opendebt.gateway.soap.skat.generated;

import java.math.BigDecimal;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {
      "fordringsType",
      "beloeb",
      "skyldnerPersonId",
      "fordringsDato",
      "forfaldsDato",
      "eksternId"
    })
@XmlRootElement(
    name = "MFFordringIndberet_IRequest",
    namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
public class MFFordringIndberet_IRequest {
  @XmlElement(
      name = "FordringsType",
      namespace = "http://skat.dk/begrebsmodel/2009/01/15/",
      required = true)
  private String fordringsType;

  @XmlElement(
      name = "Beloeb",
      namespace = "http://skat.dk/begrebsmodel/2009/01/15/",
      required = true)
  private BigDecimal beloeb;

  @XmlElement(
      name = "SkyldnerPersonId",
      namespace = "http://skat.dk/begrebsmodel/2009/01/15/",
      required = true)
  private String skyldnerPersonId;

  @XmlElement(
      name = "FordringsDato",
      namespace = "http://skat.dk/begrebsmodel/2009/01/15/",
      required = true)
  private String fordringsDato;

  @XmlElement(
      name = "ForfaldsDato",
      namespace = "http://skat.dk/begrebsmodel/2009/01/15/",
      required = true)
  private String forfaldsDato;

  @XmlElement(name = "EksternId", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private String eksternId;

  public String getFordringsType() {
    return fordringsType;
  }

  public void setFordringsType(String v) {
    this.fordringsType = v;
  }

  public BigDecimal getBeloeb() {
    return beloeb;
  }

  public void setBeloeb(BigDecimal v) {
    this.beloeb = v;
  }

  public String getSkyldnerPersonId() {
    return skyldnerPersonId;
  }

  public void setSkyldnerPersonId(String v) {
    this.skyldnerPersonId = v;
  }

  public String getFordringsDato() {
    return fordringsDato;
  }

  public void setFordringsDato(String v) {
    this.fordringsDato = v;
  }

  public String getForfaldsDato() {
    return forfaldsDato;
  }

  public void setForfaldsDato(String v) {
    this.forfaldsDato = v;
  }

  public String getEksternId() {
    return eksternId;
  }

  public void setEksternId(String v) {
    this.eksternId = v;
  }
}
