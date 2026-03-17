package dk.ufst.opendebt.creditor.dto;

import lombok.*;

/** Summary claim counts for the creditor dashboard. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimCountsDto {

  private long inRecovery;
  private long inHearing;
  private long rejected;
  private long zeroBalance;
}
