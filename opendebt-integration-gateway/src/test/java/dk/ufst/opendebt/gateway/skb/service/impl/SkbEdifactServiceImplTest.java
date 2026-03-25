package dk.ufst.opendebt.gateway.skb.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.gateway.skb.model.DebitAdvice;
import dk.ufst.opendebt.gateway.skb.model.SkbMessage;

class SkbEdifactServiceImplTest {

  private SkbEdifactServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new SkbEdifactServiceImpl();
  }

  @Test
  void parseCremul_returnsMessageWithCremulType() {
    String cremulContent = "UNB+UNOC:3+SKB+OPENDEBT+20251201+0000++CREMUL'";
    ByteArrayInputStream stream =
        new ByteArrayInputStream(cremulContent.getBytes(StandardCharsets.UTF_8));

    SkbMessage message = service.parseCremul(stream);

    assertThat(message).isNotNull();
    assertThat(message.getMessageType()).isEqualTo(SkbMessage.MessageType.CREMUL);
    assertThat(message.getCreditAdvices()).isNotNull();
  }

  @Test
  void parseCremul_handlesEmptyInput() {
    ByteArrayInputStream stream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

    SkbMessage message = service.parseCremul(stream);

    assertThat(message).isNotNull();
    assertThat(message.getMessageType()).isEqualTo(SkbMessage.MessageType.CREMUL);
    assertThat(message.getCreditAdvices()).isEmpty();
  }

  @Test
  void generateDebmul_producesValidEdifactStructure() {
    List<DebitAdvice> advices =
        List.of(
            DebitAdvice.builder()
                .paymentReference("PAY-001")
                .amount(new BigDecimal("5000.00"))
                .currency("DKK")
                .valueDate(LocalDate.of(2025, 12, 15))
                .creditorReference("CRED-001")
                .build(),
            DebitAdvice.builder()
                .paymentReference("PAY-002")
                .amount(new BigDecimal("3000.50"))
                .currency("DKK")
                .valueDate(LocalDate.of(2025, 12, 15))
                .creditorReference("CRED-002")
                .build());

    byte[] result = service.generateDebmul(advices);

    assertThat(result).isNotNull();
    String content = new String(result, StandardCharsets.UTF_8);

    // Verify EDIFACT structure
    assertThat(content)
        .contains("UNB+UNOC:3+OPENDEBT+SKB+")
        .contains("DEBMUL")
        .contains("LIN+PAY-001")
        .contains("MOA+9:5000.00:DKK")
        .contains("LIN+PAY-002")
        .contains("MOA+9:3000.50:DKK")
        .contains("DTM+203:20251215:102")
        .contains("RFF+PQ:CRED-001")
        .contains("RFF+PQ:CRED-002")
        .contains("UNZ+2+"); // trailer with message count
  }

  @Test
  void generateDebmul_defaultsToDkkWhenCurrencyIsNull() {
    List<DebitAdvice> advices =
        List.of(
            DebitAdvice.builder()
                .paymentReference("PAY-003")
                .amount(new BigDecimal("1000"))
                .currency(null) // No currency specified
                .valueDate(LocalDate.of(2025, 12, 1))
                .creditorReference("CRED-003")
                .build());

    byte[] result = service.generateDebmul(advices);
    String content = new String(result, StandardCharsets.UTF_8);

    assertThat(content).contains("MOA+9:1000:DKK");
  }

  @Test
  void generateDebmul_emptyList_producesHeaderAndTrailerOnly() {
    byte[] result = service.generateDebmul(List.of());

    assertThat(result).isNotNull();
    String content = new String(result, StandardCharsets.UTF_8);

    assertThat(content).contains("UNB+").contains("UNZ+0+").doesNotContain("LIN+");
  }

  @Test
  void generateDebmul_outputIsUtf8Encoded() {
    List<DebitAdvice> advices =
        List.of(
            DebitAdvice.builder()
                .paymentReference("PAY-004")
                .amount(new BigDecimal("100"))
                .currency("DKK")
                .valueDate(LocalDate.now())
                .creditorReference("REF-004")
                .build());

    byte[] result = service.generateDebmul(advices);

    // Should be valid UTF-8
    String content = new String(result, StandardCharsets.UTF_8);
    assertThat(content).isNotEmpty();
    assertThat(result).isEqualTo(content.getBytes(StandardCharsets.UTF_8));
  }
}
