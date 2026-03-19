package dk.ufst.opendebt.debtservice.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;

import dk.ufst.opendebt.common.audit.AuditableEntity;

import lombok.*;

@Entity
@Table(
    name = "batch_job_executions",
    indexes = {
      @Index(name = "idx_batch_job_name", columnList = "job_name"),
      @Index(name = "idx_batch_execution_date", columnList = "execution_date")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchJobExecutionEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "job_name", nullable = false, length = 100)
  private String jobName;

  @Column(name = "execution_date", nullable = false)
  private LocalDate executionDate;

  @Builder.Default
  @Column(name = "started_at", nullable = false)
  private Instant startedAt = Instant.now();

  @Column(name = "completed_at")
  private Instant completedAt;

  @Builder.Default
  @Column(name = "records_processed", nullable = false)
  private int recordsProcessed = 0;

  @Builder.Default
  @Column(name = "records_failed", nullable = false)
  private int recordsFailed = 0;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private BatchStatus status = BatchStatus.RUNNING;

  public enum BatchStatus {
    RUNNING,
    COMPLETED,
    FAILED
  }
}
