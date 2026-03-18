package dk.ufst.opendebt.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaseDebtDto {

  private UUID id;
  private UUID caseId;
  private UUID debtId;
  private LocalDateTime addedAt;
  private String addedBy;
  private LocalDateTime removedAt;
  private String removedBy;
  private String transferReference;
  private String notes;
}
