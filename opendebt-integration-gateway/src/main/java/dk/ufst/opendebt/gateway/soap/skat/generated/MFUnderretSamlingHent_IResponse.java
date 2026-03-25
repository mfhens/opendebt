package dk.ufst.opendebt.gateway.soap.skat.generated;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(
    name = "MFUnderretSamlingHent_IResponse",
    namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
public class MFUnderretSamlingHent_IResponse {
  @XmlElement(name = "FordringsId", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private String fordringsId;

  @XmlElement(name = "Underretninger", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private Underretninger underretninger = new Underretninger();

  @XmlElement(name = "Total", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
  private int total;

  public String getFordringsId() {
    return fordringsId;
  }

  public void setFordringsId(String v) {
    this.fordringsId = v;
  }

  public Underretninger getUnderretninger() {
    return underretninger;
  }

  public void setUnderretninger(Underretninger v) {
    this.underretninger = v;
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int v) {
    this.total = v;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Underretninger {
    @XmlElement(name = "Underretning", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
    private List<Underretning> underretning = new ArrayList<>();

    public List<Underretning> getUnderretning() {
      return underretning;
    }

    public void setUnderretning(List<Underretning> v) {
      this.underretning = v;
    }
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Underretning {
    @XmlElement(name = "UnderretningId", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
    private String underretningId;

    @XmlElement(name = "Type", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
    private String type;

    @XmlElement(name = "Status", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
    private String status;

    @XmlElement(name = "OprettetDato", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
    private String oprettetDato;

    @XmlElement(name = "Beskrivelse", namespace = "http://skat.dk/begrebsmodel/2009/01/15/")
    private String beskrivelse;

    public String getUnderretningId() {
      return underretningId;
    }

    public void setUnderretningId(String v) {
      this.underretningId = v;
    }

    public String getType() {
      return type;
    }

    public void setType(String v) {
      this.type = v;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String v) {
      this.status = v;
    }

    public String getOprettetDato() {
      return oprettetDato;
    }

    public void setOprettetDato(String v) {
      this.oprettetDato = v;
    }

    public String getBeskrivelse() {
      return beskrivelse;
    }

    public void setBeskrivelse(String v) {
      this.beskrivelse = v;
    }
  }
}
