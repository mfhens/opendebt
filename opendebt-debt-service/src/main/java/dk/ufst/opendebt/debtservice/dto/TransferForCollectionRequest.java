package dk.ufst.opendebt.debtservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransferForCollectionRequest {

  @NotNull(message = "Recipient (restanceinddrivelsesmyndighed) ID is required")
  private UUID recipientId;
}
