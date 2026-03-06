package dk.ufst.opendebt.payment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class IncomingPaymentDtoTest {

  @Test
  void builderAndToStringExposeValues() {
    IncomingPaymentDto dto =
        IncomingPaymentDto.builder()
            .ocrLine("OCR-123")
            .amount(new BigDecimal("100.50"))
            .currency("DKK")
            .valueDate(LocalDate.of(2026, 3, 6))
            .cremulReference("CREMUL-1")
            .build();

    assertThat(dto.getOcrLine()).isEqualTo("OCR-123");
    assertThat(dto.getAmount()).isEqualByComparingTo("100.50");
    assertThat(dto.getCurrency()).isEqualTo("DKK");
    assertThat(dto.getValueDate()).isEqualTo(LocalDate.of(2026, 3, 6));
    assertThat(dto.getCremulReference()).isEqualTo("CREMUL-1");
    assertThat(dto.toString()).contains("OCR-123", "CREMUL-1");
  }

  @Test
  void constructorsAndSettersWork() {
    IncomingPaymentDto dto = new IncomingPaymentDto();
    dto.setOcrLine("OCR-SET");
    dto.setAmount(new BigDecimal("200"));
    dto.setCurrency("EUR");
    dto.setValueDate(LocalDate.of(2026, 4, 1));
    dto.setCremulReference("CREMUL-2");

    IncomingPaymentDto copied =
        new IncomingPaymentDto(
            dto.getOcrLine(),
            dto.getAmount(),
            dto.getCurrency(),
            dto.getValueDate(),
            dto.getCremulReference());

    assertThat(copied.getOcrLine()).isEqualTo("OCR-SET");
    assertThat(copied.getCurrency()).isEqualTo("EUR");
    assertThat(copied.getAmount()).isEqualByComparingTo("200");
  }

  @Test
  void equalsAndHashCodeConsiderAllFields() {
    IncomingPaymentDto base =
        new IncomingPaymentDto(
            "OCR-123", new BigDecimal("100"), "DKK", LocalDate.of(2026, 3, 6), "CREMUL-1");
    IncomingPaymentDto same =
        new IncomingPaymentDto(
            "OCR-123", new BigDecimal("100"), "DKK", LocalDate.of(2026, 3, 6), "CREMUL-1");

    assertThat(base).isEqualTo(base);
    assertThat(base).isEqualTo(same);
    assertThat(base.hashCode()).isEqualTo(same.hashCode());
    assertThat(base).isNotEqualTo(null);
    assertThat(base).isNotEqualTo("other");
    assertThat(base)
        .isNotEqualTo(
            new IncomingPaymentDto(
                "OCR-999", new BigDecimal("100"), "DKK", LocalDate.of(2026, 3, 6), "CREMUL-1"));
    assertThat(base)
        .isNotEqualTo(
            new IncomingPaymentDto(
                "OCR-123", new BigDecimal("101"), "DKK", LocalDate.of(2026, 3, 6), "CREMUL-1"));
    assertThat(base)
        .isNotEqualTo(
            new IncomingPaymentDto(
                "OCR-123", new BigDecimal("100"), "EUR", LocalDate.of(2026, 3, 6), "CREMUL-1"));
    assertThat(base)
        .isNotEqualTo(
            new IncomingPaymentDto(
                "OCR-123", new BigDecimal("100"), "DKK", LocalDate.of(2026, 3, 7), "CREMUL-1"));
    assertThat(base)
        .isNotEqualTo(
            new IncomingPaymentDto(
                "OCR-123", new BigDecimal("100"), "DKK", LocalDate.of(2026, 3, 6), "CREMUL-9"));
  }

  @Test
  void equalsAndHashCodeHandleNullFieldsAndSubclass() {
    IncomingPaymentDto base = new IncomingPaymentDto(null, null, null, null, null);
    IncomingPaymentDto same = new IncomingPaymentDto(null, null, null, null, null);
    IncomingPaymentDto withAmount =
        new IncomingPaymentDto(null, new BigDecimal("1"), null, null, null);
    SpecialIncomingPaymentDto subclass = new SpecialIncomingPaymentDto();

    assertThat(base).isEqualTo(same);
    assertThat(base.hashCode()).isEqualTo(same.hashCode());
    assertThat(base).isNotEqualTo(withAmount);
    assertThat(base).isEqualTo(subclass);
    assertThat(subclass).isEqualTo(base);
  }

  private static final class SpecialIncomingPaymentDto extends IncomingPaymentDto {}
}
