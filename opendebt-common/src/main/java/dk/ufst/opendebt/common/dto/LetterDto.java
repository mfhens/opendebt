package dk.ufst.opendebt.common.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LetterDto {

  private UUID id;

  private UUID caseId;

  private String debtorId;

  private String templateCode;

  private String templateVersion;

  private LetterType letterType;

  private LetterStatus status;

  private DeliveryChannel deliveryChannel;

  private String recipientAddress;

  private String recipientEmail;

  private Map<String, Object> templateVariables;

  private String generatedContent;

  private LocalDateTime scheduledAt;

  private LocalDateTime sentAt;

  private LocalDateTime deliveredAt;

  private String deliveryTrackingId;

  private String failureReason;

  private LocalDateTime createdAt;

  private String createdBy;

  public enum LetterType {
    DEBT_NOTIFICATION,
    PAYMENT_REMINDER,
    WAGE_GARNISHMENT_NOTICE,
    OFFSETTING_NOTICE,
    PAYMENT_CONFIRMATION,
    PAYMENT_PLAN_PROPOSAL,
    PAYMENT_PLAN_CONFIRMATION,
    CASE_CLOSURE,
    APPEAL_ACKNOWLEDGEMENT
  }

  public enum LetterStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    SCHEDULED,
    GENERATING,
    SENT,
    DELIVERED,
    FAILED,
    CANCELLED
  }

  public enum DeliveryChannel {
    DIGITAL_POST,
    EMAIL,
    PHYSICAL_MAIL,
    E_BOKS
  }
}
