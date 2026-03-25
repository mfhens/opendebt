package dk.ufst.opendebt.gateway.soap.oio.generated;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MFUnderretSamlingHent_IRequest", namespace = "urn:oio:skat:efi:ws:1.0.1")
public class MFUnderretSamlingHent_IRequest {
  @XmlElement(name = "FordringsId", namespace = "urn:oio:skat:efi:ws:1.0.1", required = true)
  private String fordringsId;

  @XmlElement(name = "SkyldnerPersonId", namespace = "urn:oio:skat:efi:ws:1.0.1")
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
