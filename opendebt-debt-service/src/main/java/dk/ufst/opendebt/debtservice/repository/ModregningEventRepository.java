package dk.ufst.opendebt.debtservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.ModregningEvent;

@Repository
public interface ModregningEventRepository extends JpaRepository<ModregningEvent, UUID> {
  Optional<ModregningEvent> findByNemkontoReferenceId(String nemkontoReferenceId);

  List<ModregningEvent> findByDebtorPersonId(UUID debtorPersonId, Pageable pageable);
}
