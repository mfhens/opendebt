package dk.ufst.opendebt.wagegarnishment.limitation;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WageGarnishmentLimitationFacts {

  private UUID debtorPersonId;
  private Boolean decisionRegistered;
  private LocalDate debtorNotificationDate;
  private List<UUID> coveredFordringIds;
  private LocalDate inactiveSince;
}
