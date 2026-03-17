package dk.ufst.opendebt.creditor.dto;

import java.util.UUID;

import lombok.*;

/**
 * Result of debtor verification against person-registry or external CVR service. Used by the claim
 * creation wizard (Step 1) to display verification outcome.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtorVerificationResultDto {

  private boolean verified;
  private String displayName;
  private UUID personId;
  private String errorMessage;
}
