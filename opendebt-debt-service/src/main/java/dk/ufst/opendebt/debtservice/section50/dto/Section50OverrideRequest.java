package dk.ufst.opendebt.debtservice.section50.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record Section50OverrideRequest(
    @NotBlank String overrideReason,
    @NotBlank String legalBasis,
    Boolean expedited,
    List<String> selectedClaimOrder) {}
