package dk.ufst.opendebt.caseservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;

import lombok.*;

/** Links a debt to a case. Soft-delete via {@code removedAt}. */
@Entity
@Table(
    name = "case_debts",
    indexes = {
      @Index(name = "idx_case_debts_case_id", columnList = "case_id"),
      @Index(name = "idx_case_debts_debt_id", columnList = "debt_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseDebtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "case_id", nullable = false)
  private UUID caseId;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @CurrentTimestamp(event = EventType.INSERT, source = SourceType.VM)
  @Column(name = "added_at", nullable = false, updatable = false)
  private LocalDateTime addedAt;

  @Column(name = "added_by", length = 100)
  private String addedBy;

  @Column(name = "removed_at")
  private LocalDateTime removedAt;

  @Column(name = "removed_by", length = 100)
  private String removedBy;

  @Column(name = "transfer_reference", length = 200)
  private String transferReference;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;
}
