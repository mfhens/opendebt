package dk.ufst.opendebt.debtservice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;

import dk.ufst.opendebt.debtservice.service.PaymentType;

import lombok.*;

@Entity
@Table(
    name = "modregning_event",
    indexes = {
      @Index(name = "idx_me_debtor", columnList = "debtor_person_id"),
      @Index(name = "idx_me_decision_date", columnList = "decision_date")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModregningEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "nemkonto_reference_id", nullable = false, unique = true, length = 100)
  private String nemkontoReferenceId;

  @Column(name = "debtor_person_id", nullable = false)
  private UUID debtorPersonId;

  @Column(name = "receipt_date", nullable = false)
  private LocalDate receiptDate;

  @Column(name = "decision_date", nullable = false)
  private LocalDate decisionDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_type", nullable = false, length = 50)
  private PaymentType paymentType;

  @Column(name = "indkomst_aar")
  private Integer indkomstAar;

  @Column(name = "disbursement_amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal disbursementAmount;

  @Builder.Default
  @Column(name = "tier1_amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal tier1Amount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "tier2_amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal tier2Amount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "tier3_amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal tier3Amount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "residual_payout_amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal residualPayoutAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "tier2_waiver_applied", nullable = false)
  private boolean tier2WaiverApplied = false;

  @Builder.Default
  @Column(name = "notice_delivered", nullable = false)
  private boolean noticeDelivered = false;

  @Column(name = "notice_delivery_date")
  private LocalDate noticeDeliveryDate;

  @Column(name = "klage_frist_dato", nullable = false)
  private LocalDate klageFristDato;

  @Column(name = "rente_godtgoerelse_start_date")
  private LocalDate renteGodtgoerelseStartDate;

  @Builder.Default
  @Column(name = "rente_godtgoerelse_non_taxable", nullable = false)
  private boolean renteGodtgoerelseNonTaxable = true;
}
