package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CitizenEffectiveInterestRateDto {

  private String interestRuleCode;
  private BigDecimal annualRate;
  private LocalDate validFrom;
}
