package dk.ufst.opendebt.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO representing a single ledger entry for API responses. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryDto {

  private UUID id;
  private UUID transactionId;
  private UUID debtId;
  private String accountCode;
  private String accountName;
  private String entryType;
  private BigDecimal amount;
  private LocalDate effectiveDate;
  private LocalDate postingDate;
  private String reference;
  private String description;
  private String entryCategory;
  private UUID reversalOfTransactionId;
  private LocalDateTime createdAt;
}
