package dk.ufst.opendebt.debtservice.section50.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import dk.ufst.opendebt.common.audit.AuditableEntity;
import dk.ufst.opendebt.debtservice.section50.Section50ClaimCategory;
import dk.ufst.opendebt.debtservice.section50.Section50ItemType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "section50_worklist_entry",
    indexes = {
      @Index(name = "idx_s50_entry_worklist", columnList = "worklist_id"),
      @Index(name = "idx_s50_entry_rank", columnList = "rank_order")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section50WorklistEntryEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "worklist_id", nullable = false)
  private UUID worklistId;

  @Column(name = "rank_order", nullable = false)
  private int rankOrder;

  @Column(name = "claim_id", nullable = false, length = 100)
  private String claimId;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_type", nullable = false, length = 20)
  private Section50ItemType itemType;

  @Enumerated(EnumType.STRING)
  @Column(name = "claim_category", nullable = false, length = 50)
  private Section50ClaimCategory claimCategory;

  @Builder.Default
  @Column(name = "suspected_data_error", nullable = false)
  private boolean suspectedDataError = false;

  @Builder.Default
  @Column(name = "confirmed_retskraft", nullable = false)
  private boolean confirmedRetskraft = false;

  @Builder.Default
  @Column(name = "within_amount_window", nullable = false)
  private boolean withinAmountWindow = false;

  @Column(name = "selection_reason", length = 500)
  private String selectionReason;

  @Column(name = "prioritisation_factors", length = 1000)
  private String prioritisationFactors;

  @Column(name = "suppressed_reason", length = 500)
  private String suppressedReason;

  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;
}
