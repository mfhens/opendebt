package dk.ufst.opendebt.debtservice.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BeskrivelseSafeValidatorTest {

  private BeskrivelseSafeValidator validator;

  @BeforeEach
  void setUp() {
    validator = new BeskrivelseSafeValidator();
  }

  @Test
  void nullIsValid() {
    assertThat(validator.isValid(null, null)).isTrue();
  }

  @Test
  void emptyStringIsValid() {
    assertThat(validator.isValid("", null)).isTrue();
  }

  @Test
  void shortTextIsValid() {
    assertThat(validator.isValid("Restskat for 2024", null)).isTrue();
  }

  @Test
  void exactly100CharsIsValid() {
    String text = "A".repeat(100);
    assertThat(validator.isValid(text, null)).isTrue();
  }

  @Test
  void over100CharsIsInvalid() {
    String text = "A".repeat(101);
    assertThat(validator.isValid(text, null)).isFalse();
  }

  @Test
  void textWithCprNumberIsInvalid() {
    assertThat(validator.isValid("Krav vedr. 0102031234", null)).isFalse();
  }

  @Test
  void textWithPartialCprIsValid() {
    assertThat(validator.isValid("Krav vedr. 010203", null)).isTrue();
  }

  @Test
  void textWithDashSeparatedDigitsIsValid() {
    assertThat(validator.isValid("Krav 0102-031234", null)).isTrue();
  }

  @Test
  void textWithNineDigitsIsValid() {
    assertThat(validator.isValid("Reference 123456789", null)).isTrue();
  }

  @Test
  void textWithElevenDigitsContainsCpr() {
    assertThat(validator.isValid("Nr 12345678901", null)).isFalse();
  }

  @Test
  void danishCharsAreValid() {
    assertThat(validator.isValid("Daginstitution æøå ÆØÅ", null)).isTrue();
  }
}
