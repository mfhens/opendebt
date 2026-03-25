package dk.ufst.opendebt.common.dto.soap;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KvitteringResponse {
  private String kvitteringId;
  private String claimId;
  private String status;
  private Instant modtagetDato;
  private Instant behandletDato;
  private String afvisningKode;
  private String afvisningTekst;
}
