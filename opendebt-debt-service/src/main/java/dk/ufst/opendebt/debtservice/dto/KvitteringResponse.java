package dk.ufst.opendebt.debtservice.dto;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** Kvittering (receipt) response returned after fordring submission or validation. */
@Data
@Builder
public class KvitteringResponse {

  private UUID fordringsId;
  private SlutstatusEnum slutstatus;
  private List<UUID> haeftelsesforhold;
  private String akrNummer;
  private String afvistBegrundelse;
  private Integer afvistErrorCode;
  private HoeringInfoDto hoeringInfo;
}
