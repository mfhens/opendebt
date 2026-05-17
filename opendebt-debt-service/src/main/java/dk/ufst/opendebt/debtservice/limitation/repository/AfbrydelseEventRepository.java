package dk.ufst.opendebt.debtservice.limitation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ufst.opendebt.debtservice.limitation.entity.AfbrydelseEvent;

public interface AfbrydelseEventRepository extends JpaRepository<AfbrydelseEvent, UUID> {

  List<AfbrydelseEvent> findByFordringIdOrderByEventDateAsc(UUID fordringId);
}
