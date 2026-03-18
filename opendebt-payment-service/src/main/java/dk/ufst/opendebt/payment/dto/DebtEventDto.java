package dk.ufst.opendebt.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO representing a debt event for API responses. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtEventDto {

  private UUID id;
  private UUID debtId;
  private String eventType;
  private LocalDate effectiveDate;
  private BigDecimal amount;
  private UUID correctsEventId;
  private String reference;
  private String description;
  private UUID ledgerTransactionId;
  private LocalDateTime createdAt;
}
