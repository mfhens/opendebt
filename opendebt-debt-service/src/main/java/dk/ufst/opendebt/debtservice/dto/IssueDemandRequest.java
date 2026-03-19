package dk.ufst.opendebt.debtservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class IssueDemandRequest {

  @NotNull(message = "Creditor org ID is required")
  private UUID creditorOrgId;
}
