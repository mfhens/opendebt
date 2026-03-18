package dk.ufst.opendebt.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaseJournalEntryDto {

  private UUID id;
  private UUID caseId;
  private String journalEntryTitle;
  private LocalDateTime journalEntryTime;
  private UUID documentId;
  private String documentDirection;
  private String documentType;
  private String confidentialTitle;
  private String registeredBy;
  private LocalDateTime createdAt;
}
