package dk.ufst.opendebt.debtservice.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ObjectionDto {

  private UUID id;
  private UUID debtId;
  private UUID debtorPersonId;
  private String reason;
  private String status;
  private Instant registeredAt;
  private Instant resolvedAt;
  private String resolutionNote;
}
