package dk.ufst.opendebt.payment.daekning.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ufst.opendebt.payment.daekning.entity.DaekningRecord;

public interface DaekningRecordRepository extends JpaRepository<DaekningRecord, UUID> {

  List<DaekningRecord> findByDebtorId(String debtorId);

  List<DaekningRecord> findByFordringId(String fordringId);

  List<DaekningRecord> findByDebtorIdOrderByCreatedAtAsc(String debtorId);
}
