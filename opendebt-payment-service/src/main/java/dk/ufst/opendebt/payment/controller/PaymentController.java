package dk.ufst.opendebt.payment.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.payment.dto.IncomingPaymentDto;
import dk.ufst.opendebt.payment.dto.PaymentMatchResult;
import dk.ufst.opendebt.payment.service.PaymentMatchingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment processing and matching")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentMatchingService paymentMatchingService;

  @PostMapping("/incoming")
  @Operation(
      summary = "Process an incoming CREMUL payment",
      description =
          "Attempts automatic matching of an incoming payment using the Betalingsservice"
              + " OCR-linje. If the OCR-linje uniquely identifies a debt, the payment is"
              + " auto-matched and the debt is written down. Otherwise, the payment is routed"
              + " to manual matching on the case.")
  @PreAuthorize("hasRole('SYSTEM') or hasRole('ADMIN') or hasRole('SERVICE')")
  public ResponseEntity<PaymentMatchResult> processIncomingPayment(
      @Valid @RequestBody IncomingPaymentDto incomingPayment) {
    PaymentMatchResult result = paymentMatchingService.processIncomingPayment(incomingPayment);
    return ResponseEntity.ok(result);
  }
}
