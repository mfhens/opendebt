package dk.ufst.opendebt.gateway.soap.skat.generated;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(
    name = "MFUnderretSamlingHent_IRequest",
    namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
public class MFUnderretSamlingHent_IRequest {
  @XmlElement(
      name = "FordringsId",
      namespace = "http://skat.dk/begrebsmodel/2009/01/15/",
      required = true)
  private String fordringsId;

  @XmlElement(name = "SkyldnerPersonId", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private String skyldnerPersonId;

  public String getFordringsId() {
    return fordringsId;
  }

  public void setFordringsId(String v) {
    this.fordringsId = v;
  }

  public String getSkyldnerPersonId() {
    return skyldnerPersonId;
  }

  public void setSkyldnerPersonId(String v) {
    this.skyldnerPersonId = v;
  }
}
