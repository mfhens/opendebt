package dk.ufst.opendebt.debtservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.KorrektionspuljeEntry;

@Repository
public interface KorrektionspuljeEntryRepository
    extends JpaRepository<KorrektionspuljeEntry, UUID> {
  List<KorrektionspuljeEntry>
      findBySettledAtIsNullAndCorrectionPoolTargetAndAnnualOnlySettlementFalse(
          String correctionPoolTarget);

  List<KorrektionspuljeEntry> findBySettledAtIsNullAndCorrectionPoolTarget(
      String correctionPoolTarget);
}
