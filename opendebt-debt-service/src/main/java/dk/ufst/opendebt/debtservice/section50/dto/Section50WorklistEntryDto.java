package dk.ufst.opendebt.debtservice.section50.dto;

import java.math.BigDecimal;
import java.util.List;

import dk.ufst.opendebt.debtservice.section50.Section50ClaimCategory;
import dk.ufst.opendebt.debtservice.section50.Section50ItemType;

public record Section50WorklistEntryDto(
    int rank,
    String claimId,
    Section50ItemType itemType,
    Section50ClaimCategory claimCategory,
    boolean suspectedDataError,
    boolean confirmedRetskraft,
    boolean withinAmountWindow,
    String selectionReason,
    List<String> prioritisationFactors,
    String suppressedReason,
    BigDecimal amount) {}
