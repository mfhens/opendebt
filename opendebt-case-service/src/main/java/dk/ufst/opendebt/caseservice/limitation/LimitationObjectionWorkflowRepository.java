package dk.ufst.opendebt.caseservice.limitation;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LimitationObjectionWorkflowRepository
    extends JpaRepository<LimitationObjectionWorkflowRecord, UUID> {}
