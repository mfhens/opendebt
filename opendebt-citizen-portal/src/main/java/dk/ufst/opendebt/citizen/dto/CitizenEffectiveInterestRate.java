package dk.ufst.opendebt.citizen.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CitizenEffectiveInterestRate(
    String interestRuleCode, BigDecimal annualRate, LocalDate validFrom) {}
