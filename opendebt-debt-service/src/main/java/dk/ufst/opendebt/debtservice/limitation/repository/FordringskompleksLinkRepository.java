package dk.ufst.opendebt.debtservice.limitation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ufst.opendebt.debtservice.limitation.entity.FordringskompleksLink;
import dk.ufst.opendebt.debtservice.limitation.entity.FordringskompleksLinkId;

public interface FordringskompleksLinkRepository
    extends JpaRepository<FordringskompleksLink, FordringskompleksLinkId> {

  List<FordringskompleksLink> findByKompleksId(UUID kompleksId);
}
