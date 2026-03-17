package dk.ufst.opendebt.creditor.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;

/**
 * DTO representing a single claim row in the hearing claims list. Maps to the 10 required columns
 * defined in petition 031.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HearingClaimListItemDto {

  private UUID claimId;
  private LocalDateTime reportingTimestamp;
  private String debtorType;
  private String debtorIdentifier;
  private int debtorCount;
  private String creditorReference;
  private String claimTypeName;
  private String errorDescription;
  private int errorCount;
  private String hearingStatus;
  private UUID caseId;
  private String actionCode;
}
