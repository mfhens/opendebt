package dk.ufst.opendebt.debtservice.section50.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dk.ufst.opendebt.debtservice.section50.Section50ContextType;
import dk.ufst.opendebt.debtservice.section50.Section50ModregningOutcome;
import dk.ufst.opendebt.debtservice.section50.Section50OrderingMode;

public record Section50WorklistDto(
    UUID worklistId,
    UUID debtorId,
    Section50OrderingMode orderingMode,
    String legalReference,
    Section50ContextType contextType,
    BigDecimal amountWindow,
    Instant generatedAt,
    String selectedNextItemId,
    String overrideReason,
    String overrideLegalBasis,
    String deviationReason,
    Section50ModregningOutcome modregningOutcome,
    List<Section50WorklistEntryDto> entries,
    Section50DecisionSnapshotDto decisionSnapshot) {}
