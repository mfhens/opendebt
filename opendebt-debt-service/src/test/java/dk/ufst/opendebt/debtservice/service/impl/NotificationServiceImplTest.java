package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.dto.NotificationDto;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.NotificationEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

  @Mock private NotificationRepository notificationRepository;
  @Mock private DebtRepository debtRepository;

  private NotificationServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final UUID CREDITOR_ORG_ID = UUID.randomUUID();
  private static final UUID DEBTOR_PERSON_ID = UUID.randomUUID();

  private DebtEntity testDebt;

  @BeforeEach
  void setUp() {
    service = new NotificationServiceImpl(notificationRepository, debtRepository);
    testDebt =
        DebtEntity.builder()
            .id(DEBT_ID)
            .debtorPersonId(DEBTOR_PERSON_ID)
            .creditorOrgId(CREDITOR_ORG_ID)
            .principalAmount(new BigDecimal("10000"))
            .dueDate(LocalDate.now().minusMonths(1))
            .debtTypeCode("RESTSKAT")
            .build();
  }

  @Test
  void issueDemandForPayment_createsPaakravNotification() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(testDebt));
    when(notificationRepository.save(any()))
        .thenAnswer(
            inv -> {
              NotificationEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });

    NotificationDto result = service.issueDemandForPayment(DEBT_ID, CREDITOR_ORG_ID);

    assertThat(result.getType()).isEqualTo("PAAKRAV");
    assertThat(result.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(result.getRecipientPersonId()).isEqualTo(DEBTOR_PERSON_ID);
    assertThat(result.getChannel()).isEqualTo("DIGITAL_POST");
    assertThat(result.getDeliveryState()).isEqualTo("PENDING");
    assertThat(result.getOcrLine()).isNotNull();
    assertThat(result.getOcrLine()).startsWith("+71<");
  }

  @Test
  void issueDemandForPayment_generatesOcrLine() {
    String ocrLine = service.generateOcrLine(testDebt);
    assertThat(ocrLine).startsWith("+71<");
    assertThat(ocrLine).endsWith("+");
    assertThat(ocrLine).hasSize(21); // +71< (4) + 16 hex chars + + (1)
  }

  @Test
  void issueDemandForPayment_debtNotFound_throws() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.issueDemandForPayment(DEBT_ID, CREDITOR_ORG_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Debt not found");
  }

  @Test
  void issueReminder_createsRykkerNotification() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(testDebt));
    when(notificationRepository.save(any()))
        .thenAnswer(
            inv -> {
              NotificationEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });

    NotificationDto result = service.issueReminder(DEBT_ID, CREDITOR_ORG_ID);

    assertThat(result.getType()).isEqualTo("RYKKER");
    assertThat(result.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(result.getRecipientPersonId()).isEqualTo(DEBTOR_PERSON_ID);
    assertThat(result.getOcrLine()).isNull();
  }

  @Test
  void issueReminder_debtNotFound_throws() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.issueReminder(DEBT_ID, CREDITOR_ORG_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Debt not found");
  }

  @Test
  void getNotificationHistory_returnsNotificationsForDebt() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(testDebt));
    NotificationEntity n1 =
        NotificationEntity.builder()
            .id(UUID.randomUUID())
            .type(NotificationEntity.NotificationType.PAAKRAV)
            .debtId(DEBT_ID)
            .senderCreditorOrgId(CREDITOR_ORG_ID)
            .recipientPersonId(DEBTOR_PERSON_ID)
            .channel(NotificationEntity.NotificationChannel.DIGITAL_POST)
            .deliveryState(NotificationEntity.DeliveryState.SENT)
            .build();

    when(notificationRepository.findByDebtIdOrderBySentAtDesc(DEBT_ID)).thenReturn(List.of(n1));

    List<NotificationDto> result = service.getNotificationHistory(DEBT_ID);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getType()).isEqualTo("PAAKRAV");
  }

  @Test
  void getNotificationHistory_debtNotFound_throws() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getNotificationHistory(DEBT_ID))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void issueDemandForPayment_setsCorrectSenderAndRecipient() {
    when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(testDebt));
    ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
    when(notificationRepository.save(captor.capture()))
        .thenAnswer(
            inv -> {
              NotificationEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });

    service.issueDemandForPayment(DEBT_ID, CREDITOR_ORG_ID);

    NotificationEntity saved = captor.getValue();
    assertThat(saved.getSenderCreditorOrgId()).isEqualTo(CREDITOR_ORG_ID);
    assertThat(saved.getRecipientPersonId()).isEqualTo(DEBTOR_PERSON_ID);
  }
}
