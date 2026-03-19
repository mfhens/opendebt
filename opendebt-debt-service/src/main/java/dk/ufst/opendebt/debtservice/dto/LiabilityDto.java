package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiabilityDto {

  private UUID id;
  private UUID debtId;
  private UUID debtorPersonId;
  private String liabilityType;
  private BigDecimal shareAmount;
  private BigDecimal sharePercentage;
  private boolean active;
}
