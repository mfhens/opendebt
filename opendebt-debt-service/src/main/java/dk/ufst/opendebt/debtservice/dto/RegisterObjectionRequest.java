package dk.ufst.opendebt.debtservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class RegisterObjectionRequest {

  @NotNull(message = "Debtor person ID is required")
  private UUID debtorPersonId;

  @NotBlank(message = "Reason is required")
  @Size(max = 500)
  private String reason;
}
