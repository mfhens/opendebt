package dk.ufst.opendebt.creditorservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.creditorservice.entity.CreditorEntity;

@Repository
public interface CreditorRepository extends JpaRepository<CreditorEntity, UUID> {

  Optional<CreditorEntity> findByExternalCreditorId(String externalCreditorId);

  boolean existsByExternalCreditorId(String externalCreditorId);

  Optional<CreditorEntity> findByCreditorOrgId(UUID creditorOrgId);

  List<CreditorEntity> findByParentCreditorId(UUID parentCreditorId);

  List<CreditorEntity> findByActivityStatusOrderByExternalCreditorIdAsc(
      dk.ufst.opendebt.creditorservice.entity.ActivityStatus activityStatus);
}
