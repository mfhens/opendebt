package dk.ufst.opendebt.debtservice.service;

import java.util.List;
import java.util.UUID;

import dk.ufst.opendebt.debtservice.dto.ObjectionDto;
import dk.ufst.opendebt.debtservice.entity.ObjectionEntity.ObjectionStatus;

public interface ObjectionService {

  ObjectionDto registerObjection(UUID debtId, UUID debtorPersonId, String reason);

  ObjectionDto resolveObjection(UUID objectionId, ObjectionStatus outcome, String note);

  boolean hasActiveObjection(UUID debtId);

  List<ObjectionDto> getObjections(UUID debtId);
}
