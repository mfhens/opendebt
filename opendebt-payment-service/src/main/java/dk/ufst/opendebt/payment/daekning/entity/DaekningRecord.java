package dk.ufst.opendebt.payment.daekning.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import dk.ufst.opendebt.payment.daekning.InddrivelsesindsatsType;
import dk.ufst.opendebt.payment.daekning.PrioritetKategori;
import dk.ufst.opendebt.payment.daekning.RenteKomponent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "daekning_record")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DaekningRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "fordring_id", length = 100, nullable = false)
  private String fordringId;

  @Column(name = "debtor_id", length = 100, nullable = false)
  private String debtorId;

  @Enumerated(EnumType.STRING)
  @Column(name = "komponent", length = 60, nullable = false)
  private RenteKomponent komponent;

  @Column(name = "daekning_beloeb", precision = 15, scale = 2, nullable = false)
  private BigDecimal daekningBeloeb;

  @Column(name = "betalingstidspunkt")
  private Instant betalingstidspunkt;

  @Column(name = "application_timestamp")
  private Instant applicationTimestamp;

  @Column(name = "gil_paragraf", length = 100)
  private String gilParagraf;

  @Enumerated(EnumType.STRING)
  @Column(name = "prioritet_kategori", length = 60)
  private PrioritetKategori prioritetKategori;

  @Column(name = "fifo_sort_key")
  private LocalDate fifoSortKey;

  @Builder.Default
  @Column(name = "udlaeg_surplus", nullable = false)
  private Boolean udlaegSurplus = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "inddrivelsesindsats_type", length = 30)
  private InddrivelsesindsatsType inddrivelsesindsatsType;

  @Column(name = "opskrivning_af_fordring_id", length = 100)
  private String opskrivningAfFordringId;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
