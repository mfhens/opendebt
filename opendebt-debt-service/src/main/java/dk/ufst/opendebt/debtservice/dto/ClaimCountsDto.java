package dk.ufst.opendebt.debtservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
