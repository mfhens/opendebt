package dk.ufst.opendebt.payment.daekning.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ufst.opendebt.payment.daekning.entity.DaekningFordringEntity;

public interface DaekningFordringRepository extends JpaRepository<DaekningFordringEntity, UUID> {

  List<DaekningFordringEntity> findByDebtorId(String debtorId);

  void deleteByDebtorId(String debtorId);
}
