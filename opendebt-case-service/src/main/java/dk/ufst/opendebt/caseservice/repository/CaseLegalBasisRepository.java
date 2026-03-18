package dk.ufst.opendebt.caseservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.caseservice.entity.CaseLegalBasisEntity;

@Repository
public interface CaseLegalBasisRepository extends JpaRepository<CaseLegalBasisEntity, UUID> {

  List<CaseLegalBasisEntity> findByCaseId(UUID caseId);
}
