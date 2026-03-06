package dk.ufst.opendebt.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.payment.dto.IncomingPaymentDto;
import dk.ufst.opendebt.payment.dto.PaymentMatchResult;
import dk.ufst.opendebt.payment.service.PaymentMatchingService;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

  @Mock private PaymentMatchingService paymentMatchingService;

  private PaymentController controller;

  @BeforeEach
  void setUp() {
    controller = new PaymentController(paymentMatchingService);
  }

  @Test
  void processIncomingPayment_returnsOk() {
    IncomingPaymentDto incomingPayment =
        IncomingPaymentDto.builder()
            .ocrLine("OCR-123")
            .amount(new BigDecimal("100"))
            .currency("DKK")
            .valueDate(LocalDate.of(2026, 3, 6))
            .cremulReference("CREMUL-1")
            .build();
    PaymentMatchResult result = PaymentMatchResult.builder().paymentId(UUID.randomUUID()).build();
    when(paymentMatchingService.processIncomingPayment(incomingPayment)).thenReturn(result);

    var response = controller.processIncomingPayment(incomingPayment);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(result);
  }
}
