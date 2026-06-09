package dk.ufst.opendebt.caseworker.section50;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PortalSection50WorklistDto(
    UUID worklistId,
    UUID debtorId,
    String orderingMode,
    String legalReference,
    String contextType,
    BigDecimal amountWindow,
    Instant generatedAt,
    String selectedNextItemId,
    String overrideReason,
    String overrideLegalBasis,
    String deviationReason,
    String modregningOutcome,
    List<PortalSection50WorklistEntryDto> entries,
    PortalSection50DecisionSnapshotDto decisionSnapshot) {}
