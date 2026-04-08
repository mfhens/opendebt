package dk.ufst.opendebt.payment.daekning.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import dk.ufst.opendebt.payment.daekning.InddrivelsesindsatsType;

public record SimulateRequestDto(
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal beloeb,
    InddrivelsesindsatsType inddrivelsesindsatsType) {}
