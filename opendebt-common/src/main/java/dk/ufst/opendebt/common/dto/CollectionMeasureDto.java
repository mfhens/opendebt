package dk.ufst.opendebt.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollectionMeasureDto {

  private UUID id;
  private UUID caseId;
  private UUID debtId;
  private String measureType;
  private String status;
  private LocalDate startDate;
  private LocalDate endDate;
  private BigDecimal amount;
}
