package dk.ufst.opendebt.debtservice.limitation.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ufst.opendebt.debtservice.limitation.entity.ForaeldelseRecord;

public interface ForaeldelseRecordRepository extends JpaRepository<ForaeldelseRecord, UUID> {

  Optional<ForaeldelseRecord> findByFordringId(UUID fordringId);

  boolean existsByKompleksId(UUID kompleksId);
}
