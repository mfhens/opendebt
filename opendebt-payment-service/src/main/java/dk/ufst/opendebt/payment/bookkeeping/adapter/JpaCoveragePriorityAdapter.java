package dk.ufst.opendebt.payment.bookkeeping.adapter;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import dk.ufst.bookkeeping.model.CoverageAllocation;
import dk.ufst.bookkeeping.port.CoveragePriorityPort;
import dk.ufst.opendebt.payment.bookkeeping.service.CoveragePriorityService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaCoveragePriorityAdapter implements CoveragePriorityPort {

  private final CoveragePriorityService coveragePriorityService;

  @Override
  public CoverageAllocation allocatePayment(
      UUID debtId,
      BigDecimal paymentAmount,
      BigDecimal accruedInterest,
      BigDecimal outstandingFees,
      BigDecimal principalBalance) {
    return coveragePriorityService.allocatePayment(
        debtId, paymentAmount, accruedInterest, outstandingFees, principalBalance);
  }
}
