package dk.ufst.opendebt.gateway.soap.skat.generated;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"fordringsId", "status"})
@XmlRootElement(
    name = "MFFordringIndberet_IResponse",
    namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
public class MFFordringIndberet_IResponse {
  @XmlElement(name = "FordringsId", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private String fordringsId;

  @XmlElement(name = "Status", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
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
