package dk.ufst.opendebt.creditor.dto;

/** Enumeration of notification types available for creditor portal search. */
public enum NotificationType {
  INTEREST("notifications.type.interest"),
  DETAILED_INTEREST("notifications.type.detailed_interest"),
  EQUALISATION("notifications.type.equalisation"),
  ALLOCATION("notifications.type.allocation"),
  SETTLEMENT("notifications.type.settlement"),
  RETURN("notifications.type.return"),
  WRITE_OFF("notifications.type.write_off");

  private final String messageKey;

  NotificationType(String messageKey) {
    this.messageKey = messageKey;
  }

  public String getMessageKey() {
    return messageKey;
  }
}
