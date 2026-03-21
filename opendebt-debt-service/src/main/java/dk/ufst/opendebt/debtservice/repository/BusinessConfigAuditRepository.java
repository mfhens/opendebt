package dk.ufst.opendebt.debtservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.BusinessConfigAuditEntity;

@Repository
public interface BusinessConfigAuditRepository
    extends JpaRepository<BusinessConfigAuditEntity, UUID> {
  List<BusinessConfigAuditEntity> findByConfigKeyOrderByPerformedAtDesc(String configKey);

  List<BusinessConfigAuditEntity> findByConfigEntryIdOrderByPerformedAtDesc(UUID configEntryId);
}
