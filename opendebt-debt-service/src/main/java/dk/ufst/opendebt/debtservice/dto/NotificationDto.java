package dk.ufst.opendebt.debtservice.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDto {

  private UUID id;
  private String type;
  private UUID debtId;
  private UUID senderCreditorOrgId;
  private UUID recipientPersonId;
  private String channel;
  private Instant sentAt;
  private String deliveryState;
  private String ocrLine;
  private UUID relatedLifecycleEventId;
}
