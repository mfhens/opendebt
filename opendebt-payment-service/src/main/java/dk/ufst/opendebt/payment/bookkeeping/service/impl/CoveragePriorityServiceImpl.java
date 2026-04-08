package dk.ufst.opendebt.payment.bookkeeping.service.impl;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dk.ufst.bookkeeping.model.CoverageAllocation;
import dk.ufst.opendebt.payment.bookkeeping.service.CoveragePriorityService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CoveragePriorityServiceImpl implements CoveragePriorityService {

  @Override
  public CoverageAllocation allocatePayment(
      UUID debtId,
      BigDecimal paymentAmount,
      BigDecimal accruedInterest,
      BigDecimal outstandingFees,
      BigDecimal principalBalance) {

    BigDecimal remaining = paymentAmount;

    // 1. Renter (interest) — always covered first
    BigDecimal interestPortion = remaining.min(accruedInterest);
    remaining = remaining.subtract(interestPortion);

    // 2. Gebyrer (fees) — covered second
    BigDecimal feesPortion = remaining.min(outstandingFees);
    remaining = remaining.subtract(feesPortion);

    // 3. Hovedstol (principal) — covered last
    BigDecimal principalPortion = remaining.min(principalBalance);

    log.debug(
        "DAEKNINGSRAEKKEFOELGE: debtId={}, payment={}, interest={}, fees={}, principal={} "
            + "-> interestPortion={}, feesPortion={}, principalPortion={}",
        debtId,
        paymentAmount,
        accruedInterest,
        outstandingFees,
        principalBalance,
        interestPortion,
        feesPortion,
        principalPortion);

    return CoverageAllocation.builder()
        .debtId(debtId)
        .totalAmount(paymentAmount)
        .interestPortion(interestPortion)
        .feesPortion(feesPortion)
        .principalPortion(principalPortion)
        .accruedInterestAtDate(accruedInterest)
        .outstandingFeesAtDate(outstandingFees)
        .principalBalanceAtDate(principalBalance)
        .build();
  }
}
