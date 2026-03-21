package dk.ufst.opendebt.debtservice.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.FeeEntity;

@Repository
public interface FeeRepository extends JpaRepository<FeeEntity, UUID> {

  List<FeeEntity> findByDebtIdAndPaidFalse(UUID debtId);

  @Query(
      "SELECT COALESCE(SUM(f.amount), 0) FROM FeeEntity f WHERE f.debtId = :debtId AND f.paid = false")
  BigDecimal sumUnpaidByDebtId(@Param("debtId") UUID debtId);
}
