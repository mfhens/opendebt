package dk.ufst.opendebt.gateway.soap.skat.generated;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"fordringsId"})
@XmlRootElement(
    name = "MFKvitteringHent_IRequest",
    namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
public class MFKvitteringHent_IRequest {
  @XmlElement(
      name = "FordringsId",
      namespace = "http://skat.dk/begrebsmodel/2009/01/15/",
      required = true)
  private String fordringsId;

  public String getFordringsId() {
    return fordringsId;
  }

  public void setFordringsId(String v) {
    this.fordringsId = v;
  }
}
