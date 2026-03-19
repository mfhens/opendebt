package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollectionMeasureDto {

  private UUID id;
  private UUID debtId;
  private String measureType;
  private String status;
  private String initiatedBy;
  private Instant initiatedAt;
  private Instant completedAt;
  private BigDecimal amount;
  private String note;
}
