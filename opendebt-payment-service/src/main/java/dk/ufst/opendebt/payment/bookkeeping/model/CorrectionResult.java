package dk.ufst.opendebt.payment.bookkeeping.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.*;

/** Result of a retroactive correction, detailing all storno and new entries posted. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrectionResult {

  private UUID debtId;
  private UUID correctionEventId;
  private BigDecimal principalDelta;
  private int stornoEntriesPosted;
  private int newInterestEntriesPosted;
  private BigDecimal oldInterestTotal;
  private BigDecimal newInterestTotal;
  private BigDecimal interestDelta;
  private List<InterestPeriod> recalculatedPeriods;
}
