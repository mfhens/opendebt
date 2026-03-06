package dk.ufst.opendebt.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an incoming payment from SKB CREMUL. The {@code ocrLine} field carries the
 * Betalingsservice OCR-linje used for automatic matching against debts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingPaymentDto {

  /** Betalingsservice OCR-linje for matching against debts. */
  private String ocrLine;

  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be positive")
  private BigDecimal amount;

  private String currency;

  @NotNull(message = "Value date is required")
  private LocalDate valueDate;

  @NotBlank(message = "CREMUL reference is required")
  private String cremulReference;
}
