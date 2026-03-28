package dk.ufst.opendebt.debtservice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;

import dk.ufst.opendebt.common.audit.AuditableEntity;

import lombok.*;

/** Individual fee imposed during collection. */
@Entity
@Table(
    name = "fees",
    indexes = {
      @Index(name = "idx_fee_debt_id", columnList = "debt_id"),
      @Index(name = "idx_fee_accrual_date", columnList = "accrual_date")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Enumerated(EnumType.STRING)
  @Column(name = "fee_type", nullable = false, length = 30)
  private FeeType feeType;

  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(name = "accrual_date", nullable = false)
  private LocalDate accrualDate;

  @Column(name = "legal_basis", length = 500)
  private String legalBasis;

  @Column(name = "paid", nullable = false)
  @Builder.Default
  private boolean paid = false;

  public enum FeeType {
    RYKKER,
    UDLAEG,
    LOENINDEHOLDELSE,
    /** Tilsigelsesgebyr — fee for summoning debtor to udlægsforretning. GIL § 6, stk. 1. */
    TILSIGELSE,
    OTHER
  }
}
