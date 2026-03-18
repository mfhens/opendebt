package dk.ufst.opendebt.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ObjectionDto {

  private UUID id;
  private UUID caseId;
  private UUID debtId;
  private String objectionType;
  private String status;
  private String description;
  private LocalDateTime receivedAt;
  private LocalDateTime resolvedAt;
}
