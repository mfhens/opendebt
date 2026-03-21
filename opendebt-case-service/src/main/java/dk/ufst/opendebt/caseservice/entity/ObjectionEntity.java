package dk.ufst.opendebt.caseservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;

import lombok.*;

/** A debtor objection (indsigelse) against a case or specific debt. */
@Entity
@Table(
    name = "objections",
    indexes = {
      @Index(name = "idx_objections_case_id", columnList = "case_id"),
      @Index(name = "idx_objections_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "case_id", nullable = false)
  private UUID caseId;

  @Column(name = "debt_id")
  private UUID debtId;

  @Enumerated(EnumType.STRING)
  @Column(name = "objection_type", nullable = false, length = 20)
  private ObjectionType objectionType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ObjectionStatus status;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "debtor_statement", columnDefinition = "TEXT")
  private String debtorStatement;

  @Column(name = "caseworker_assessment", columnDefinition = "TEXT")
  private String caseworkerAssessment;

  @Column(name = "received_at", nullable = false)
  private LocalDateTime receivedAt;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "resolved_by", length = 100)
  private String resolvedBy;

  @CurrentTimestamp(event = EventType.INSERT, source = SourceType.VM)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
