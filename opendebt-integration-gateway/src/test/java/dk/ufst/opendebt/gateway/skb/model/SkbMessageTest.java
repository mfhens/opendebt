package dk.ufst.opendebt.gateway.skb.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class SkbMessageTest {

  @Test
  void builderAndAccessorsWork() {
    CreditAdvice creditAdvice =
        CreditAdvice.builder()
            .messageReference("CREDIT-1")
            .valueDate(LocalDate.of(2026, 3, 6))
            .build();
    DebitAdvice debitAdvice =
        DebitAdvice.builder()
            .messageReference("DEBIT-1")
            .valueDate(LocalDate.of(2026, 3, 7))
            .build();
    LocalDateTime preparedAt = LocalDateTime.of(2026, 3, 6, 12, 0);

    SkbMessage message =
        SkbMessage.builder()
            .messageType(SkbMessage.MessageType.CREMUL)
            .interchangeReference("INT-1")
            .preparationDateTime(preparedAt)
            .senderIdentification("OPENDEBT")
            .recipientIdentification("SKB")
            .creditAdvices(List.of(creditAdvice))
            .debitAdvices(List.of(debitAdvice))
            .build();

    assertThat(message.getMessageType()).isEqualTo(SkbMessage.MessageType.CREMUL);
    assertThat(message.getInterchangeReference()).isEqualTo("INT-1");
    assertThat(message.getPreparationDateTime()).isEqualTo(preparedAt);
    assertThat(message.getSenderIdentification()).isEqualTo("OPENDEBT");
    assertThat(message.getRecipientIdentification()).isEqualTo("SKB");
    assertThat(message.getCreditAdvices()).hasSize(1);
    assertThat(message.getDebitAdvices()).hasSize(1);
  }

  @Test
  void constructorsAndSettersWork() {
    SkbMessage message = new SkbMessage();
    message.setMessageType(SkbMessage.MessageType.DEBMUL);
    message.setInterchangeReference("INT-2");
    message.setPreparationDateTime(LocalDateTime.of(2026, 3, 7, 9, 0));
    message.setSenderIdentification("A");
    message.setRecipientIdentification("B");
    message.setCreditAdvices(List.of());
    message.setDebitAdvices(List.of());

    SkbMessage copied =
        new SkbMessage(
            message.getMessageType(),
            message.getInterchangeReference(),
            message.getPreparationDateTime(),
            message.getSenderIdentification(),
            message.getRecipientIdentification(),
            message.getCreditAdvices(),
            message.getDebitAdvices());

    assertThat(copied.getMessageType()).isEqualTo(SkbMessage.MessageType.DEBMUL);
    assertThat(copied.getInterchangeReference()).isEqualTo("INT-2");
  }
}
