package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity.MeasureType;

import lombok.Data;

@Data
public class InitiateMeasureRequest {

  @NotNull(message = "Measure type is required")
  private MeasureType measureType;

  private BigDecimal amount;
  private String note;
}
