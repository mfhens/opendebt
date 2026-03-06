package dk.ufst.opendebt.gateway.skb.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

/** Represents a single debit transaction for generating a DEBMUL EDIFACT message. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebitAdvice {

  private String messageReference;
  private String accountNumber;
  private BigDecimal amount;
  private String currency;
  private LocalDate valueDate;
  private String creditorReference;
  private String paymentReference;
  private String remittanceInfo;
}
