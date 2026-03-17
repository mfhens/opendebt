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
public class InterestSelectionEmbeddable {

  @Column(name = "interest_rule", length = 10)
  private String interestRule;

  @Column(name = "interest_rate_code", length = 10)
  private String interestRateCode;

  @Column(name = "additional_interest_rate", precision = 10, scale = 4)
  private BigDecimal additionalInterestRate;
}
