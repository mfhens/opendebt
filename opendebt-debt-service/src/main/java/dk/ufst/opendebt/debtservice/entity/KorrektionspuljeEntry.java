package dk.ufst.opendebt.debtservice.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(
    name = "korrektionspulje_entry",
    indexes = {@Index(name = "idx_kpe_debtor", columnList = "debtor_person_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KorrektionspuljeEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "debtor_person_id", nullable = false)
  private UUID debtorPersonId;

  @Column(name = "origin_event_id", nullable = false)
  private UUID originEventId;

  @Column(name = "surplus_amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal surplusAmount;

  @Column(name = "correction_pool_target", nullable = false, length = 10)
  private String correctionPoolTarget;

  @Builder.Default
  @Column(name = "boerne_ydelse_restriction", nullable = false)
  private boolean boerneYdelseRestriction = false;

  @Column(name = "rente_godtgoerelse_start_date")
  private LocalDate renteGodtgoerelseStartDate;

  @Builder.Default
  @Column(name = "rente_godtgoerelse_accrued", nullable = false, precision = 15, scale = 2)
  private BigDecimal renteGodtgoerelseAccrued = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "annual_only_settlement", nullable = false)
  private boolean annualOnlySettlement = false;

  @Column(name = "settled_at")
  private Instant settledAt;

  @Builder.Default
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
