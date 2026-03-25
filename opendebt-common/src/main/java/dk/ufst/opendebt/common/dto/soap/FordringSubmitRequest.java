package dk.ufst.opendebt.common.dto.soap;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FordringSubmitRequest {
  private String claimType;
  private String debtorPersonId;
  private BigDecimal amount;
  private LocalDate claimDate;
  private LocalDate dueDate;
  private String externalId;
}
