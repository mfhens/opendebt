package dk.ufst.opendebt.gateway.soap.oio.generated;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"fordringsId", "status"})
@XmlRootElement(name = "MFFordringIndberet_IResponse", namespace = "urn:oio:skat:efi:ws:1.0.1")
public class MFFordringIndberet_IResponse {
  @XmlElement(name = "FordringsId", namespace = "urn:oio:skat:efi:ws:1.0.1")
  private String fordringsId;

  @XmlElement(name = "Status", namespace = "urn:oio:skat:efi:ws:1.0.1")
  private String status;

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
}
