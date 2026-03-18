package dk.ufst.opendebt.payment.bookkeeping.service.impl;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.payment.bookkeeping.model.CoverageAllocation;
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
      BigDecimal principalBalance) {

    BigDecimal interestPortion;
    BigDecimal principalPortion;

    if (paymentAmount.compareTo(accruedInterest) <= 0) {
      interestPortion = paymentAmount;
      principalPortion = BigDecimal.ZERO;
    } else {
      interestPortion = accruedInterest;
      BigDecimal remainder = paymentAmount.subtract(accruedInterest);
      principalPortion = remainder.min(principalBalance);
    }

    log.debug(
        "DAEKNINGSRAEKKEFOELGE: debtId={}, payment={}, interest={}, principal={} "
            + "-> interestPortion={}, principalPortion={}",
        debtId,
        paymentAmount,
        accruedInterest,
        principalBalance,
        interestPortion,
        principalPortion);

    return CoverageAllocation.builder()
        .debtId(debtId)
        .totalAmount(paymentAmount)
        .interestPortion(interestPortion)
        .principalPortion(principalPortion)
        .accruedInterestAtDate(accruedInterest)
        .principalBalanceAtDate(principalBalance)
        .build();
  }
}
