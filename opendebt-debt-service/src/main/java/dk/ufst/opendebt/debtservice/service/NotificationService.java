package dk.ufst.opendebt.debtservice.service;

import java.util.List;
import java.util.UUID;

import dk.ufst.opendebt.debtservice.dto.NotificationDto;

public interface NotificationService {

  NotificationDto issueDemandForPayment(UUID debtId, UUID creditorOrgId);

  NotificationDto issueReminder(UUID debtId, UUID creditorOrgId);

  List<NotificationDto> getNotificationHistory(UUID debtId);
}
