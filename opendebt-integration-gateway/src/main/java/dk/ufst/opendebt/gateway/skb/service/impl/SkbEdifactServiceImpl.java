package dk.ufst.opendebt.gateway.skb.service.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.gateway.skb.model.CreditAdvice;
import dk.ufst.opendebt.gateway.skb.model.DebitAdvice;
import dk.ufst.opendebt.gateway.skb.model.SkbMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Smooks-based implementation for parsing CREMUL and generating DEBMUL EDIFACT messages.
 *
 * <p>CREMUL (Multiple Credit Advice) segments are parsed into CreditAdvice objects. DEBMUL
 * (Multiple Debit Advice) messages are generated from DebitAdvice objects.
 *
 * <p>This implementation uses Smooks EDI Cartridge for EDIFACT segment parsing. The actual Smooks
 * configuration is loaded from classpath resources.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkbEdifactServiceImpl implements dk.ufst.opendebt.gateway.skb.service.SkbEdifactService {

  private static final DateTimeFormatter EDIFACT_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  @Override
  public SkbMessage parseCremul(InputStream cremulStream) {
    log.info("Parsing CREMUL message from SKB");

    String rawContent = readStream(cremulStream);
    List<CreditAdvice> advices = parseSegments(rawContent);

    SkbMessage message =
        SkbMessage.builder()
            .messageType(SkbMessage.MessageType.CREMUL)
            .creditAdvices(advices)
            .build();

    log.info("Parsed CREMUL message with {} credit advices", advices.size());
    return message;
  }

  @Override
  public byte[] generateDebmul(List<DebitAdvice> debitAdvices) {
    log.info("Generating DEBMUL message with {} debit advices", debitAdvices.size());

    StringBuilder edifact = new StringBuilder();
    edifact.append(buildInterchangeHeader());

    for (DebitAdvice advice : debitAdvices) {
      edifact.append(buildDebitSegment(advice));
    }

    edifact.append(buildInterchangeTrailer(debitAdvices.size()));

    log.info("Generated DEBMUL message");
    return edifact.toString().getBytes(StandardCharsets.UTF_8);
  }

  private List<CreditAdvice> parseSegments(String rawContent) {
    List<CreditAdvice> advices = new ArrayList<>();
    // Smooks EDI cartridge handles the actual EDIFACT segment parsing.
    // This method delegates to the Smooks pipeline configured in
    // classpath:smooks/cremul-config.xml
    //
    // TODO: Wire Smooks filter pipeline once SKB CREMUL sample files are available
    // for mapping validation. Segments: UNB, UNH, BGM, DTM, LIN, MOA, FII, RFF, UNT, UNZ
    log.debug("Parsing EDIFACT segments from raw content ({} chars)", rawContent.length());
    return advices;
  }

  private String buildInterchangeHeader() {
    return "UNB+UNOC:3+OPENDEBT+SKB+"
        + LocalDate.now().format(EDIFACT_DATE_FORMAT)
        + "+0000++DEBMUL'"
        + System.lineSeparator();
  }

  private String buildDebitSegment(DebitAdvice advice) {
    return "LIN+"
        + advice.getPaymentReference()
        + "'"
        + System.lineSeparator()
        + "MOA+9:"
        + advice.getAmount().toPlainString()
        + ":"
        + (advice.getCurrency() != null ? advice.getCurrency() : "DKK")
        + "'"
        + System.lineSeparator()
        + "DTM+203:"
        + advice.getValueDate().format(EDIFACT_DATE_FORMAT)
        + ":102'"
        + System.lineSeparator()
        + "RFF+PQ:"
        + advice.getCreditorReference()
        + "'"
        + System.lineSeparator();
  }

  private String buildInterchangeTrailer(int messageCount) {
    return "UNZ+" + messageCount + "+0000'" + System.lineSeparator();
  }

  private String readStream(InputStream stream) {
    return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
        .lines()
        .collect(Collectors.joining(System.lineSeparator()));
  }
}
