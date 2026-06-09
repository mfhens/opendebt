package dk.ufst.opendebt.caseworker.section50;

import java.math.BigDecimal;
import java.util.List;

public record PortalSection50WorklistEntryDto(
    int rank,
    String claimId,
    String itemType,
    String claimCategory,
    boolean suspectedDataError,
    boolean confirmedRetskraft,
    boolean withinAmountWindow,
    String selectionReason,
    List<String> prioritisationFactors,
    String suppressedReason,
    BigDecimal amount) {}
