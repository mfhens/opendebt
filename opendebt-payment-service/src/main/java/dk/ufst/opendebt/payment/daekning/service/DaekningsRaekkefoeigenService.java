package dk.ufst.opendebt.payment.daekning.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import dk.ufst.opendebt.payment.daekning.InddrivelsesindsatsType;
import dk.ufst.opendebt.payment.daekning.dto.DaekningsraekkefoelgePositionDto;
import dk.ufst.opendebt.payment.daekning.dto.SimulatePositionDto;
import dk.ufst.opendebt.payment.daekning.entity.DaekningRecord;

public interface DaekningsRaekkefoeigenService {

  /**
   * Returns the ordered list of positions for a debtor as of asOf date.
   *
   * @param debtorId debtor identifier
   * @param asOf date to compute ordering as-of (null = today)
   * @return ordered list of positions
   */
  List<DaekningsraekkefoelgePositionDto> getOrdering(String debtorId, LocalDate asOf);

  /**
   * Simulates payment application without DB writes.
   *
   * @return simulated positions with daekning amounts
   */
  List<SimulatePositionDto> simulate(
      String debtorId,
      BigDecimal beloeb,
      InddrivelsesindsatsType inddrivelsesindsatsType,
      Instant applicationTimestamp);

  /**
   * Applies payment: creates DaekningRecord entries.
   *
   * @return list of DaekningRecords created
   */
  List<DaekningRecord> apply(
      String debtorId,
      BigDecimal beloeb,
      InddrivelsesindsatsType inddrivelsesindsatsType,
      Instant betalingstidspunkt,
      Instant applicationTimestamp);
}
