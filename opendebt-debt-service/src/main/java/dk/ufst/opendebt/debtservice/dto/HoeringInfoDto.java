package dk.ufst.opendebt.debtservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** Hearing details included in kvittering when slutstatus is HOERING. */
@Data
@Builder
public class HoeringInfoDto {

  private UUID hoeringId;
  private String deviationDescription;
  private LocalDateTime slaDeadline;
}
