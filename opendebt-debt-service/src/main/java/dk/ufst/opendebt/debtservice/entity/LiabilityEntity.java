package dk.ufst.opendebt.debtservice.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.*;

import dk.ufst.opendebt.common.audit.AuditableEntity;

import lombok.*;

@Entity
@Table(
    name = "liabilities",
    indexes = {
      @Index(name = "idx_liability_debt_id", columnList = "debt_id"),
      @Index(name = "idx_liability_debtor", columnList = "debtor_person_id")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_liability_debt_debtor",
          columnNames = {"debt_id", "debtor_person_id"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiabilityEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Column(name = "debtor_person_id", nullable = false)
  private UUID debtorPersonId;

  @Enumerated(EnumType.STRING)
  @Column(name = "liability_type", nullable = false, length = 30)
  private LiabilityType liabilityType;

  @Column(name = "share_amount", precision = 15, scale = 2)
  private BigDecimal shareAmount;

  @Column(name = "share_percentage", precision = 5, scale = 2)
  private BigDecimal sharePercentage;

  @Builder.Default
  @Column(name = "active", nullable = false)
  private boolean active = true;

  public enum LiabilityType {
    SOLE,
    JOINT_AND_SEVERAL,
    PROPORTIONAL
  }
}
