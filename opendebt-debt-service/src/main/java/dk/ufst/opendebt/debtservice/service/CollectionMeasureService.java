package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dk.ufst.opendebt.debtservice.dto.CollectionMeasureDto;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity.MeasureType;

public interface CollectionMeasureService {

  CollectionMeasureDto initiateMeasure(
      UUID debtId, MeasureType type, BigDecimal amount, String note);

  CollectionMeasureDto completeMeasure(UUID measureId);

  CollectionMeasureDto cancelMeasure(UUID measureId, String reason);

  List<CollectionMeasureDto> getMeasures(UUID debtId);
}
