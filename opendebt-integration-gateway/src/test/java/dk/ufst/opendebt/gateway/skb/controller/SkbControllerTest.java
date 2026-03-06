package dk.ufst.opendebt.gateway.skb.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import dk.ufst.opendebt.gateway.skb.model.DebitAdvice;
import dk.ufst.opendebt.gateway.skb.model.SkbMessage;
import dk.ufst.opendebt.gateway.skb.service.SkbEdifactService;

@ExtendWith(MockitoExtension.class)
class SkbControllerTest {

  @Mock private SkbEdifactService skbEdifactService;

  private SkbController controller;

  @BeforeEach
  void setUp() {
    controller = new SkbController(skbEdifactService);
  }

  @Test
  void parseCremul_returnsParsedMessage() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "cremul.edi", "application/edifact", "UNH".getBytes());
    SkbMessage message = SkbMessage.builder().messageType(SkbMessage.MessageType.CREMUL).build();
    when(skbEdifactService.parseCremul(any())).thenReturn(message);

    var response = controller.parseCremul(file);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(message);
  }

  @Test
  void generateDebmul_returnsAttachmentBody() {
    List<DebitAdvice> debitAdvices =
        List.of(
            DebitAdvice.builder()
                .messageReference("MSG-1")
                .valueDate(LocalDate.of(2026, 3, 6))
                .build());
    byte[] payload = "DEBMUL".getBytes(StandardCharsets.UTF_8);
    when(skbEdifactService.generateDebmul(debitAdvices)).thenReturn(payload);

    var response = controller.generateDebmul(debitAdvices);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getHeaders().getFirst("Content-Disposition"))
        .isEqualTo("attachment; filename=\"debmul.edi\"");
    assertThat(response.getBody()).isEqualTo(payload);
  }
}
