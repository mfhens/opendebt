package dk.ufst.opendebt.debtservice.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.RenteGodtgoerelseRateEntry;

@Repository
public interface RenteGodtgoerelseRateEntryRepository
    extends JpaRepository<RenteGodtgoerelseRateEntry, UUID> {
  Optional<RenteGodtgoerelseRateEntry>
      findFirstByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(LocalDate date);
}
