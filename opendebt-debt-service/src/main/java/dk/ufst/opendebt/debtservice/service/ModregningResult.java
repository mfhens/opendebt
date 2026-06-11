package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ModregningResult(
    String decisionReference,
    String lineageReference,
    ModregningDecisionKind decisionKind,
    boolean operative,
    String supersedesDecisionReference,
    boolean hasHistory,
    UUID eventId,
    UUID debtorPersonId,
    LocalDate decisionDate,
    BigDecimal disbursementAmount,
    BigDecimal tier1Amount,
    BigDecimal tier2Amount,
    BigDecimal tier3Amount,
    BigDecimal residualPayoutAmount,
    boolean tier2WaiverApplied,
    boolean noticeDelivered,
    LocalDate noticeDeliveryDate,
    LocalDate klageFristDato,
    LocalDate renteGodtgoerelseStartDate,
    boolean renteGodtgoerelseNonTaxable,
    List<FordringCoverageDto> coverages) {}
