package dk.ufst.opendebt.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaseEventDto {

  private UUID id;
  private UUID caseId;
  private UUID debtId;
  private String eventType;
  private String description;
  private String metadata;
  private String performedBy;
  private LocalDateTime performedAt;
}
