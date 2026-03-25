package dk.ufst.opendebt.gateway.soap.skat.generated;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(
    name = "MFKvitteringHent_IResponse",
    namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
public class MFKvitteringHent_IResponse {
  @XmlElement(name = "KvitteringId", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private String kvitteringId;

  @XmlElement(name = "FordringsId", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private String fordringsId;

  @XmlElement(name = "Status", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private String status;

  @XmlElement(name = "ModtagetDato", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private String modtagetDato;

  @XmlElement(name = "BehandletDato", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private String behandletDato;

  @XmlElement(name = "AfvisningKode", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private String afvisningKode;

  @XmlElement(name = "AfvisningTekst", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private String afvisningTekst;

  public String getKvitteringId() {
    return kvitteringId;
  }

  public void setKvitteringId(String v) {
    this.kvitteringId = v;
  }

  public String getFordringsId() {
    return fordringsId;
  }

  public void setFordringsId(String v) {
    this.fordringsId = v;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String v) {
    this.status = v;
  }

  public String getModtagetDato() {
    return modtagetDato;
  }

  public void setModtagetDato(String v) {
    this.modtagetDato = v;
  }

  public String getBehandletDato() {
    return behandletDato;
  }

  public void setBehandletDato(String v) {
    this.behandletDato = v;
  }

  public String getAfvisningKode() {
    return afvisningKode;
  }

  public void setAfvisningKode(String v) {
    this.afvisningKode = v;
  }

  public String getAfvisningTekst() {
    return afvisningTekst;
  }

  public void setAfvisningTekst(String v) {
    this.afvisningTekst = v;
  }
}
