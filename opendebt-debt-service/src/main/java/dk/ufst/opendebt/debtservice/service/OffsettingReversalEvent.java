package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.util.UUID;

public record OffsettingReversalEvent(
    UUID originModregningEventId,
    UUID reversedFordringId,
    BigDecimal surplusAmount,
    UUID debtorPersonId,
    String correctionPoolTarget,
    String originalPaymentType,
    boolean debtUnderCollectionOptOut,
    boolean retroactivePartialCoverage) {}
