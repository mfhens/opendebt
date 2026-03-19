package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import dk.ufst.opendebt.debtservice.entity.LiabilityEntity.LiabilityType;

import lombok.Data;

@Data
public class AddLiabilityRequest {

  @NotNull(message = "Debtor person ID is required")
  private UUID debtorPersonId;

  @NotNull(message = "Liability type is required")
  private LiabilityType liabilityType;

  private BigDecimal sharePercentage;
}
