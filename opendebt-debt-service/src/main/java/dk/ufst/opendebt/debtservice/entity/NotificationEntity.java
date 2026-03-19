package dk.ufst.opendebt.debtservice.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import dk.ufst.opendebt.common.audit.AuditableEntity;

import lombok.*;

@Entity
@Table(
    name = "notifications",
    indexes = {
      @Index(name = "idx_notification_debt_id", columnList = "debt_id"),
      @Index(name = "idx_notification_recipient", columnList = "recipient_person_id"),
      @Index(name = "idx_notification_type", columnList = "notification_type"),
      @Index(name = "idx_notification_delivery_state", columnList = "delivery_state")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false, length = 30)
  private NotificationType type;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Column(name = "sender_creditor_org_id", nullable = false)
  private UUID senderCreditorOrgId;

  @Column(name = "recipient_person_id", nullable = false)
  private UUID recipientPersonId;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 20)
  private NotificationChannel channel;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_state", nullable = false, length = 20)
  private DeliveryState deliveryState;

  @Column(name = "ocr_line", length = 50)
  private String ocrLine;

  @Column(name = "related_lifecycle_event_id")
  private UUID relatedLifecycleEventId;

  public enum NotificationType {
    PAAKRAV,
    RYKKER,
    AFREGNING,
    UDLIGNING,
    ALLOKERING,
    RENTER,
    AFSKRIVNING,
    TILBAGESEND
  }

  public enum NotificationChannel {
    DIGITAL_POST,
    PHYSICAL_MAIL,
    PORTAL
  }

  public enum DeliveryState {
    PENDING,
    SENT,
    DELIVERED,
    FAILED
  }
}
