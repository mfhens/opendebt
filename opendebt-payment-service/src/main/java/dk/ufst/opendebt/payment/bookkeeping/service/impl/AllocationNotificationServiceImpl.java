package dk.ufst.opendebt.payment.bookkeeping.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.payment.bookkeeping.model.*;
import dk.ufst.opendebt.payment.bookkeeping.service.AllocationNotificationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AllocationNotificationServiceImpl implements AllocationNotificationService {

  @Override
  public AllocationNotification generateFromReplayResult(
      TimelineReplayResult replayResult, UUID creditorOrgId) {

    List<AllocationNotification.AllocationLine> allocationLines = new ArrayList<>();

    for (CoverageAllocation allocation : replayResult.getRecalculatedAllocations()) {
      allocationLines.add(
          AllocationNotification.AllocationLine.builder()
              .effectiveDate(allocation.getEffectiveDate())
              .transactionType("DAEKNING")
              .totalAmount(allocation.getTotalAmount())
              .interestPortion(allocation.getInterestPortion())
              .principalPortion(allocation.getPrincipalPortion())
              .build());
    }

    List<AllocationNotification.ReversalLine> reversalLines = new ArrayList<>();

    for (CoverageReversal reversal : replayResult.getCoverageReversals()) {
      reversalLines.add(
          AllocationNotification.ReversalLine.builder()
              .effectiveDate(reversal.getEffectiveDate())
              .originalInterestPortion(reversal.getOriginalAllocation().getInterestPortion())
              .originalPrincipalPortion(reversal.getOriginalAllocation().getPrincipalPortion())
              .correctedInterestPortion(reversal.getReplacementAllocation().getInterestPortion())
              .correctedPrincipalPortion(reversal.getReplacementAllocation().getPrincipalPortion())
              .reason(reversal.getReason())
              .build());
    }

    BigDecimal interestChange =
        replayResult.getNewInterestTotal() != null
            ? replayResult.getNewInterestTotal()
            : BigDecimal.ZERO;

    AllocationNotification notification =
        AllocationNotification.builder()
            .debtId(replayResult.getDebtId())
            .creditorOrgId(creditorOrgId)
            .notificationDate(LocalDate.now())
            .generatedAt(LocalDateTime.now())
            .interestChange(interestChange)
            .principalChange(BigDecimal.ZERO)
            .newInterestBalance(replayResult.getFinalInterestBalance())
            .newPrincipalBalance(replayResult.getFinalPrincipalBalance())
            .newTotalBalance(
                replayResult.getFinalPrincipalBalance().add(replayResult.getFinalInterestBalance()))
            .allocationLines(allocationLines)
            .reversalLines(reversalLines)
            .hasCrossingReversals(!reversalLines.isEmpty())
            .build();

    log.info(
        "ALLOKERINGSUNDERRETNING: debtId={}, creditor={}, allocations={}, reversals={}",
        replayResult.getDebtId(),
        creditorOrgId,
        allocationLines.size(),
        reversalLines.size());

    return notification;
  }
}
