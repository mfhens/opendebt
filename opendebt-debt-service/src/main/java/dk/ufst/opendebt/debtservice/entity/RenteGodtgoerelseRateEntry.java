package dk.ufst.opendebt.debtservice.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(
    name = "rentegodt_rate_entry",
    indexes = {@Index(name = "idx_rgre_effective_date", columnList = "effective_date")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenteGodtgoerelseRateEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "publication_date", nullable = false, unique = true)
  private LocalDate publicationDate;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @Column(name = "reference_rate_percent", nullable = false, precision = 6, scale = 4)
  private BigDecimal referenceRatePercent;

  @Column(name = "godtgoerelse_rate_percent", nullable = false, precision = 6, scale = 4)
  private BigDecimal godtgoerelseRatePercent;

  @Builder.Default
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
