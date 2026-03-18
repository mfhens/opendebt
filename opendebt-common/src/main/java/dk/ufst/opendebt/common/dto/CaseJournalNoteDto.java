package dk.ufst.opendebt.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaseJournalNoteDto {

  private UUID id;
  private UUID caseId;
  private String noteTitle;
  private String noteText;
  private String authorId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
