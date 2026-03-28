package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.dto.NotificationDto;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.NotificationEntity;
import dk.ufst.opendebt.debtservice.entity.NotificationEntity.DeliveryState;
import dk.ufst.opendebt.debtservice.entity.NotificationEntity.NotificationChannel;
import dk.ufst.opendebt.debtservice.entity.NotificationEntity.NotificationType;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.NotificationRepository;
import dk.ufst.opendebt.debtservice.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final DebtRepository debtRepository;

  @Override
  @Transactional
  public NotificationDto issueDemandForPayment(UUID debtId, UUID creditorOrgId) {
    DebtEntity debt = findDebtOrThrow(debtId);

    String ocrLine = generateOcrLine(debt);

    NotificationEntity notification =
        NotificationEntity.builder()
            .type(NotificationType.PAAKRAV)
            .debtId(debtId)
            .senderCreditorOrgId(creditorOrgId)
            .recipientPersonId(debt.getDebtorPersonId())
            .channel(NotificationChannel.DIGITAL_POST)
            .deliveryState(DeliveryState.PENDING)
            .sentAt(Instant.now())
            .ocrLine(ocrLine)
            .build();

    notification = notificationRepository.save(notification);

    log.info(
        "Issued demand for payment (paakrav) for debt={}, notification={}, ocr={}",
        debtId,
        notification.getId(),
        ocrLine);

    return toDto(notification);
  }

  @Override
  @Transactional
  public NotificationDto issueReminder(UUID debtId, UUID creditorOrgId) {
    DebtEntity debt = findDebtOrThrow(debtId);

    NotificationEntity notification =
        NotificationEntity.builder()
            .type(NotificationType.RYKKER)
            .debtId(debtId)
            .senderCreditorOrgId(creditorOrgId)
            .recipientPersonId(debt.getDebtorPersonId())
            .channel(NotificationChannel.DIGITAL_POST)
            .deliveryState(DeliveryState.PENDING)
            .sentAt(Instant.now())
            .build();

    notification = notificationRepository.save(notification);

    log.info("Issued reminder (rykker) for debt={}, notification={}", debtId, notification.getId());

    return toDto(notification);
  }

  @Override
  @Transactional(readOnly = true)
  public List<NotificationDto> getNotificationHistory(UUID debtId) {
    findDebtOrThrow(debtId);
    return notificationRepository.findByDebtIdOrderBySentAtDesc(debtId).stream()
        .map(this::toDto)
        .toList();
  }

  private DebtEntity findDebtOrThrow(UUID debtId) {
    return debtRepository
        .findById(debtId)
        .orElseThrow(() -> new IllegalArgumentException("Debt not found: " + debtId));
  }

  @Override
  @Transactional
  public void notifyModregning(UUID debtId, BigDecimal offsetAmount) {
    // G.A.3.1.4: debtor must receive a modregningsmeddelelse when a SET_OFF is applied.
    // Full letter-service integration is tracked in TB-038. This stub ensures the
    // obligation is auditable until the outbound channel is implemented.
    log.warn(
        "MODREGNING_NOTIFICATION_REQUIRED: debt={}, offsetAmount={} — "
            + "modregningsmeddelelse not yet dispatched (G.A.3.1.4). See TB-038.",
        debtId,
        offsetAmount);
  }

  /** Generates a structured OCR payment reference line for demand-for-payment notifications. */
  String generateOcrLine(DebtEntity debt) {
    String debtRef = debt.getId().toString().replace("-", "").substring(0, 16).toUpperCase();
    return "+71<" + debtRef + "+";
  }

  private NotificationDto toDto(NotificationEntity entity) {
    return NotificationDto.builder()
        .id(entity.getId())
        .type(entity.getType().name())
        .debtId(entity.getDebtId())
        .senderCreditorOrgId(entity.getSenderCreditorOrgId())
        .recipientPersonId(entity.getRecipientPersonId())
        .channel(entity.getChannel().name())
        .sentAt(entity.getSentAt())
        .deliveryState(entity.getDeliveryState().name())
        .ocrLine(entity.getOcrLine())
        .relatedLifecycleEventId(entity.getRelatedLifecycleEventId())
        .build();
  }
}
