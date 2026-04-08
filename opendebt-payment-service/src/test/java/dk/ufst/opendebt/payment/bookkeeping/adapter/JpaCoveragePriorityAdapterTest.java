package dk.ufst.opendebt.payment.bookkeeping.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.bookkeeping.model.CoverageAllocation;
import dk.ufst.opendebt.payment.bookkeeping.service.CoveragePriorityService;

@ExtendWith(MockitoExtension.class)
class JpaCoveragePriorityAdapterTest {

  @Mock private CoveragePriorityService coveragePriorityService;

  @InjectMocks private JpaCoveragePriorityAdapter adapter;

  @Test
  void allocatePayment_delegatesToCoveragePriorityService() {
    UUID debtId = UUID.randomUUID();
    BigDecimal payment = new BigDecimal("1000.00");
    BigDecimal interest = new BigDecimal("50.00");
    BigDecimal fees = new BigDecimal("25.00");
    BigDecimal principal = new BigDecimal("5000.00");

    CoverageAllocation expected =
        CoverageAllocation.builder()
            .interestPortion(interest)
            .feesPortion(fees)
            .principalPortion(payment.subtract(interest).subtract(fees))
            .build();

    when(coveragePriorityService.allocatePayment(debtId, payment, interest, fees, principal))
        .thenReturn(expected);

    CoverageAllocation result = adapter.allocatePayment(debtId, payment, interest, fees, principal);

    assertThat(result).isEqualTo(expected);
    verify(coveragePriorityService).allocatePayment(debtId, payment, interest, fees, principal);
  }
}
