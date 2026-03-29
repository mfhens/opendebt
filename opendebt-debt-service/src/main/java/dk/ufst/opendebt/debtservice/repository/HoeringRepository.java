package dk.ufst.opendebt.debtservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.HoeringEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringStatus;

@Repository
public interface HoeringRepository extends JpaRepository<HoeringEntity, UUID> {

  /**
   * Returns the most recently resolved høring record for the given debt, or empty if none exists.
   * Using {@code findTop…OrderByResolvedAtDesc} avoids {@code
   * IncorrectResultSizeDataAccessException} when multiple høring records exist for the same debt
   * (NB1 fix).
   */
  Optional<HoeringEntity> findTopByDebtIdOrderByResolvedAtDesc(UUID debtId);

  Page<HoeringEntity> findByHoeringStatus(HoeringStatus status, Pageable pageable);

  List<HoeringEntity> findByHoeringStatusAndSlaDeadlineBefore(
      HoeringStatus status, LocalDateTime deadline);
}
