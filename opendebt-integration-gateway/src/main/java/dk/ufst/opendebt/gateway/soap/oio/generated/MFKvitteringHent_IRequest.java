package dk.ufst.opendebt.gateway.soap.oio.generated;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"fordringsId"})
@XmlRootElement(name = "MFKvitteringHent_IRequest", namespace = "urn:oio:skat:efi:ws:1.0.1")
public class MFKvitteringHent_IRequest {
  @XmlElement(name = "FordringsId", namespace = "urn:oio:skat:efi:ws:1.0.1", required = true)
  private String fordringsId;

  public String getFordringsId() {
    return fordringsId;
  }

  public void setFordringsId(String v) {
    this.fordringsId = v;
  }
}
