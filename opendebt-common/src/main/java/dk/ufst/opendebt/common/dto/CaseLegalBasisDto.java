package dk.ufst.opendebt.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaseLegalBasisDto {

  private UUID id;
  private UUID caseId;
  private String legalSourceUri;
  private String legalSourceTitle;
  private String paragraphReference;
  private String description;
  private LocalDateTime createdAt;
}
