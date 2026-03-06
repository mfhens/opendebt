package dk.ufst.opendebt.gateway.skb.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

/** Represents a single credit transaction parsed from a CREMUL EDIFACT message. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditAdvice {

  private String messageReference;
  private String accountNumber;
  private BigDecimal amount;
  private String currency;
  private LocalDate valueDate;
  private String debtorReference;
  private String paymentReference;
  private String remittanceInfo;
}
