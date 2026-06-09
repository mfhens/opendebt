package dk.ufst.opendebt.debtservice.section50.entity;

import java.math.BigDecimal;
import java.time.Instant;
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
import dk.ufst.opendebt.debtservice.section50.Section50ContextType;
import dk.ufst.opendebt.debtservice.section50.Section50ModregningOutcome;
import dk.ufst.opendebt.debtservice.section50.Section50OrderingMode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "section50_worklist",
    indexes = {
      @Index(name = "idx_s50_worklist_debtor", columnList = "debtor_person_id"),
      @Index(name = "idx_s50_worklist_mode", columnList = "ordering_mode")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section50WorklistEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "debtor_person_id", nullable = false)
  private UUID debtorPersonId;

  @Enumerated(EnumType.STRING)
  @Column(name = "context_type", nullable = false, length = 50)
  private Section50ContextType contextType;

  @Enumerated(EnumType.STRING)
  @Column(name = "ordering_mode", nullable = false, length = 50)
  private Section50OrderingMode orderingMode;

  @Column(name = "legal_reference", nullable = false, length = 200)
  private String legalReference;

  @Column(name = "amount_window", precision = 15, scale = 2)
  private BigDecimal amountWindow;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt;

  @Column(name = "selected_next_item_id", length = 100)
  private String selectedNextItemId;

  @Column(name = "override_reason", length = 500)
  private String overrideReason;

  @Column(name = "override_legal_basis", length = 500)
  private String overrideLegalBasis;

  @Column(name = "deviation_reason", length = 500)
  private String deviationReason;

  @Enumerated(EnumType.STRING)
  @Column(name = "modregning_outcome", length = 50)
  private Section50ModregningOutcome modregningOutcome;
}
