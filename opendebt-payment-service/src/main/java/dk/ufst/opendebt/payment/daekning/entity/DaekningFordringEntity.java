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

import dk.ufst.opendebt.payment.daekning.PrioritetKategori;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "daekning_fordring")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DaekningFordringEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "fordring_id", length = 100, nullable = false)
  private String fordringId;

  @Column(name = "debtor_id", length = 100, nullable = false)
  private String debtorId;

  @Enumerated(EnumType.STRING)
  @Column(name = "prioritet_kategori", length = 60, nullable = false)
  private PrioritetKategori prioritetKategori;

  @Column(name = "tilbaestaaende_beloeb", precision = 15, scale = 2, nullable = false)
  private BigDecimal tilbaestaaendeBeloeb;

  @Column(name = "modtagelsesdato", nullable = false)
  private LocalDate modtagelsesdato;

  @Column(name = "legacy_modtagelsesdato")
  private LocalDate legacyModtagelsesdato;

  @Column(name = "sekvens_nummer")
  private Integer sekvensNummer;

  @Column(name = "opskrivning_af_fordring_id", length = 100)
  private String opskrivningAfFordringId;

  @Column(name = "fordring_type", length = 60)
  private String fordringType;

  @Builder.Default
  @Column(name = "in_loenindeholdelse_indsats", nullable = false)
  private Boolean inLoenindeholdelsesIndsats = false;

  @Builder.Default
  @Column(name = "in_udlaeg_forretning", nullable = false)
  private Boolean inUdlaegForretning = false;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "beloeb_opkraevningsrenter", precision = 15, scale = 2)
  private BigDecimal beloebOpkraevningsrenter;

  @Column(name = "beloeb_inddrivelsesrenter_fordringshaver", precision = 15, scale = 2)
  private BigDecimal beloebInddrivelsesrenterFordringshaver;

  @Column(name = "beloeb_inddrivelsesrenter_foer_tilbagefoersel", precision = 15, scale = 2)
  private BigDecimal beloebInddrivelsesrenterFoerTilbagefoersel;

  @Column(name = "beloeb_inddrivelsesrenter_stk1", precision = 15, scale = 2)
  private BigDecimal beloebInddrivelsesrenterStk1;

  @Column(name = "beloeb_oevrige_renter_psrm", precision = 15, scale = 2)
  private BigDecimal beloebOevrigeRenterPsrm;

  @Column(name = "beloeb_hoofdfordring", precision = 15, scale = 2)
  private BigDecimal beloebHooffordring;
}
