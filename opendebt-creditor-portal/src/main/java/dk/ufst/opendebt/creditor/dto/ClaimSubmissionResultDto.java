package dk.ufst.opendebt.creditor.dto;

import java.util.List;
import java.util.UUID;

import lombok.*;

/**
 * Result of claim submission to debt-service, wrapping the outcome (UDFOERT, AFVIST, or HOERING)
 * along with any validation errors or receipt data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSubmissionResultDto {

  /** Submission outcome: UDFOERT (accepted), AFVIST (rejected), HOERING (pending hearing). */
  private String outcome;

  /** Assigned claim ID (available when outcome is UDFOERT or HOERING). */
  private UUID claimId;

  /** Processing status description. */
  private String processingStatus;

  /** Validation errors (populated when outcome is AFVIST). */
  private List<ValidationErrorDto> errors;
}
