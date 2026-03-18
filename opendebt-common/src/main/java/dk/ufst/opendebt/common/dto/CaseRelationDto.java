package dk.ufst.opendebt.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaseRelationDto {

  private UUID id;
  private UUID sourceCaseId;
  private UUID targetCaseId;
  private String relationType;
  private String description;
  private String createdBy;
  private LocalDateTime createdAt;
}
