package dk.ufst.opendebt.caseworker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Portal-facing DTO representing a debt event from payment-service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalDebtEventDto {

  private UUID id;
  private UUID debtId;
  private String eventType;
  private BigDecimal amount;
  private String description;
  private String reference;
  private String status;
  private LocalDateTime occurredAt;
  private LocalDateTime createdAt;
}
