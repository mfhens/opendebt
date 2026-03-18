package dk.ufst.opendebt.caseservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.caseservice.entity.CasePartyEntity;
import dk.ufst.opendebt.caseservice.entity.PartyRole;

@Repository
public interface CasePartyRepository extends JpaRepository<CasePartyEntity, UUID> {

  List<CasePartyEntity> findByCaseId(UUID caseId);

  List<CasePartyEntity> findByCaseIdAndPartyRole(UUID caseId, PartyRole partyRole);

  List<CasePartyEntity> findByPersonIdAndPartyRole(UUID personId, PartyRole partyRole);
}
