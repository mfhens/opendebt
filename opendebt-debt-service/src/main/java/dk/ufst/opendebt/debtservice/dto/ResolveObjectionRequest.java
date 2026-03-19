package dk.ufst.opendebt.debtservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import dk.ufst.opendebt.debtservice.entity.ObjectionEntity.ObjectionStatus;

import lombok.Data;

@Data
public class ResolveObjectionRequest {

  @NotNull(message = "Outcome is required (UPHELD or REJECTED)")
  private ObjectionStatus outcome;

  @Size(max = 500)
  private String note;
}
