package dk.ufst.opendebt.gateway.skb.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.*;

/** Wrapper for a parsed SKB EDIFACT interchange containing one or more advices. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkbMessage {

  public enum MessageType {
    CREMUL,
    DEBMUL
  }

  private MessageType messageType;
  private String interchangeReference;
  private LocalDateTime preparationDateTime;
  private String senderIdentification;
  private String recipientIdentification;
  private List<CreditAdvice> creditAdvices;
  private List<DebitAdvice> debitAdvices;
}
