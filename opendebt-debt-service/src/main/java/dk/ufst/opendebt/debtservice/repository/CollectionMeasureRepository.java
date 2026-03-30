package dk.ufst.opendebt.debtservice.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity;

@Repository
public interface CollectionMeasureRepository extends JpaRepository<CollectionMeasureEntity, UUID> {

  List<CollectionMeasureEntity> findByDebtIdOrderByInitiatedAtDesc(UUID debtId);

  /**
   * Returns the subset of the supplied {@code debtIds} that have at least one active
   * WAGE_GARNISHMENT (lønindeholdelse) measure in progress. Used by {@code
   * ActiveFordringServiceImpl} to populate {@code inLoenindeholdelsesIndsats} in a single batch
   * query, avoiding N+1 per fordring.
   */
  @Query(
      "SELECT DISTINCT c.debtId FROM CollectionMeasureEntity c "
          + "WHERE c.debtId IN :debtIds "
          + "AND c.measureType = 'WAGE_GARNISHMENT' "
          + "AND c.status = 'IN_PROGRESS'")
  Set<UUID> findActiveWageGarnishmentDebtIds(@Param("debtIds") Set<UUID> debtIds);
}
