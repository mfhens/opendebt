package dk.ufst.opendebt.debtservice.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentevalgEmbeddable {

  @Column(name = "rente_regel", length = 10)
  private String renteRegel;

  @Column(name = "rente_sats_kode", length = 10)
  private String renteSatsKode;

  @Column(name = "mer_rente_sats", precision = 10, scale = 4)
  private BigDecimal merRenteSats;
}
