package dk.ufst.opendebt.debtservice.service;

import java.util.List;
import java.util.UUID;

import dk.ufst.opendebt.debtservice.dto.KvitteringResponse;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringEntity;

/**
 * Service for building kvittering (receipt) responses after fordring submission or validation. The
 * kvittering communicates the outcome back to the fordringshaver.
 */
public interface KvitteringService {

  /**
   * Builds a kvittering response for a debt submission.
   *
   * @param debtId the debt identifier
   * @param entity the debt entity
   * @param validationErrors list of validation error messages (empty if valid)
   * @param hoering hearing entity if a hearing was triggered, null otherwise
   * @return the kvittering response
   */
  KvitteringResponse buildKvittering(
      UUID debtId, DebtEntity entity, List<String> validationErrors, HoeringEntity hoering);
}
