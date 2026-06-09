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
    name = "section50_candidate_item",
    indexes = {
      @Index(name = "idx_s50_candidate_debtor", columnList = "debtor_person_id"),
      @Index(name = "idx_s50_candidate_claim", columnList = "claim_id", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section50CandidateItemEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "debtor_person_id", nullable = false)
  private UUID debtorPersonId;

  @Column(name = "claim_id", nullable = false, length = 100)
  private String claimId;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_type", nullable = false, length = 20)
  private Section50ItemType itemType;

  @Enumerated(EnumType.STRING)
  @Column(name = "claim_category", nullable = false, length = 50)
  private Section50ClaimCategory claimCategory;

  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Builder.Default
  @Column(name = "suspected_data_error", nullable = false)
  private boolean suspectedDataError = false;

  @Builder.Default
  @Column(name = "confirmed_retskraft", nullable = false)
  private boolean confirmedRetskraft = false;

  @Column(name = "accessory_of_claim_id", length = 100)
  private String accessoryOfClaimId;

  @Builder.Default
  @Column(name = "disproportionate_write_off", nullable = false)
  private boolean disproportionateWriteOff = false;

  @Column(name = "error_type", length = 100)
  private String errorType;

  @Column(name = "complexity", length = 20)
  private String complexity;

  @Column(name = "payment_opportunity", length = 20)
  private String paymentOpportunity;
}
