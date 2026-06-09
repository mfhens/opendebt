package dk.ufst.opendebt.caseworker.section50;

import java.util.List;

public record Section50OverrideSubmission(
    String overrideReason, String legalBasis, Boolean expedited, List<String> selectedClaimOrder) {}
