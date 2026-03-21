package dk.ufst.opendebt.debtservice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;

import lombok.*;

/** Time-versioned business configuration entry with explicit validity period. */
@Entity
@Table(
    name = "business_config",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_config_key_valid_from",
          columnNames = {"config_key", "valid_from"})
    },
    indexes = {
      @Index(name = "idx_config_key", columnList = "config_key"),
      @Index(name = "idx_config_valid_from", columnList = "valid_from")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessConfigEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "config_key", nullable = false, length = 100)
  private String configKey;

  @Column(name = "config_value", nullable = false, length = 500)
  private String configValue;

  @Column(name = "value_type", nullable = false, length = 20)
  private String valueType;

  @Column(name = "valid_from", nullable = false)
  private LocalDate validFrom;

  @Column(name = "valid_to")
  private LocalDate validTo;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "legal_basis", length = 500)
  private String legalBasis;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @CurrentTimestamp(event = EventType.INSERT, source = SourceType.VM)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Version private Long version;

  /** Parse configValue as BigDecimal. */
  public BigDecimal getDecimalValue() {
    return new BigDecimal(configValue);
  }
}
