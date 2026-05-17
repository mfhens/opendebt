package dk.ufst.opendebt.debtservice.limitation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ufst.opendebt.debtservice.limitation.entity.TillaegsfristEvent;

public interface TillaegsfristEventRepository extends JpaRepository<TillaegsfristEvent, UUID> {

  List<TillaegsfristEvent> findByFordringIdOrderByAppliedDateAsc(UUID fordringId);
}
