package dk.ufst.opendebt.debtservice.section50.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import dk.ufst.opendebt.debtservice.section50.Section50ContextType;

public record GenerateSection50WorklistRequest(
    @NotNull Section50ContextType contextType,
    BigDecimal availableAmount,
    BigDecimal confirmedAmountCovered,
    List<String> candidateClaimIds,
    Boolean requestedBySystem) {}
