package dk.ufst.opendebt.debtservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleEvent;

@Repository
public interface ClaimLifecycleEventRepository extends JpaRepository<ClaimLifecycleEvent, UUID> {

  List<ClaimLifecycleEvent> findByDebtIdOrderByOccurredAtDesc(UUID debtId);
}
