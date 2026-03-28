package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dk.ufst.opendebt.debtservice.dto.NotificationDto;

public interface NotificationService {

  NotificationDto issueDemandForPayment(UUID debtId, UUID creditorOrgId);

  NotificationDto issueReminder(UUID debtId, UUID creditorOrgId);

  List<NotificationDto> getNotificationHistory(UUID debtId);

  /**
   * Records a modregning notification obligation for the debtor per G.A.3.1.4.
   *
   * <p>The debtor must be informed of a SET_OFF (modregning) in an offentlig udbetaling. This stub
   * logs the obligation; full letter-service integration is tracked in TB-038 area.
   *
   * @param debtId the debt against which modregning was performed
   * @param offsetAmount the amount offset
   */
  void notifyModregning(UUID debtId, BigDecimal offsetAmount);
}
