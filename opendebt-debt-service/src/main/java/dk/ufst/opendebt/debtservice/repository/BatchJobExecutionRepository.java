package dk.ufst.opendebt.debtservice.repository;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;

@Repository
public interface BatchJobExecutionRepository extends JpaRepository<BatchJobExecutionEntity, UUID> {

  boolean existsByJobNameAndExecutionDate(String jobName, LocalDate executionDate);
}
