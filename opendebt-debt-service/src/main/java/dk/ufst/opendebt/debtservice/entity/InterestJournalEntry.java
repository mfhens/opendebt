package dk.ufst.opendebt.debtservice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;

import dk.ufst.opendebt.common.audit.AuditableEntity;
import dk.ufst.opendebt.common.dto.AccountingTarget;

import lombok.*;

@Entity
@Table(
    name = "interest_journal_entries",
    indexes = {
      @Index(name = "idx_interest_debt_id", columnList = "debt_id"),
      @Index(name = "idx_interest_accrual_date", columnList = "accrual_date")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_interest_debt_date",
          columnNames = {"debt_id", "accrual_date"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestJournalEntry extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Column(name = "accrual_date", nullable = false)
  private LocalDate accrualDate;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @Column(name = "balance_snapshot", nullable = false, precision = 15, scale = 2)
  private BigDecimal balanceSnapshot;

  @Column(name = "rate", nullable = false, precision = 5, scale = 4)
  private BigDecimal rate;

  @Column(name = "interest_amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal interestAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "accounting_target", length = 20)
  private AccountingTarget accountingTarget;
}
