package dk.ufst.opendebt.debtservice.section50.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Section50DecisionSnapshotDto(
    UUID decisionId,
    UUID worklistId,
    String rulePath,
    String inputHash,
    String selectedNextItemId,
    String legalReference,
    UUID auditEventId,
    String origin,
    Instant occurredAt,
    String notes,
    List<String> prioritisationFactors) {}
