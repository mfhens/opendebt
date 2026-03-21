package dk.ufst.opendebt.caseservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;

import lombok.*;

/** Directed relationship between two cases. */
@Entity
@Table(
    name = "case_relations",
    indexes = {
      @Index(name = "idx_case_relations_source", columnList = "source_case_id"),
      @Index(name = "idx_case_relations_target", columnList = "target_case_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseRelationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "source_case_id", nullable = false)
  private UUID sourceCaseId;

  @Column(name = "target_case_id", nullable = false)
  private UUID targetCaseId;

  @Enumerated(EnumType.STRING)
  @Column(name = "relation_type", nullable = false, length = 20)
  private CaseRelationType relationType;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @CurrentTimestamp(event = EventType.INSERT, source = SourceType.VM)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
