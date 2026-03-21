package dk.ufst.opendebt.caseservice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;

import lombok.*;

/** A collection measure (inddrivelsesskridt) applied to a case or specific debt. */
@Entity
@Table(
    name = "collection_measures",
    indexes = {
      @Index(name = "idx_collection_measures_case_id", columnList = "case_id"),
      @Index(name = "idx_collection_measures_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionMeasureEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "case_id", nullable = false)
  private UUID caseId;

  @Column(name = "debt_id")
  private UUID debtId;

  @Enumerated(EnumType.STRING)
  @Column(name = "measure_type", nullable = false, length = 30)
  private MeasureType measureType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private MeasureStatus status;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "amount", precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(name = "reference", length = 200)
  private String reference;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @CurrentTimestamp(event = EventType.INSERT, source = SourceType.VM)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @CurrentTimestamp(source = SourceType.VM)
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
