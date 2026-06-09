package dk.ufst.opendebt.debtservice.section50.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import dk.ufst.opendebt.debtservice.section50.Section50ModregningOutcome;

public record Section50ModregningDecisionRequest(
    @NotNull Section50ModregningOutcome modregningOutcome, @NotBlank String reason) {}
