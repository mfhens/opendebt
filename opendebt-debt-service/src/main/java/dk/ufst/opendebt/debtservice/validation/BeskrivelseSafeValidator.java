package dk.ufst.opendebt.debtservice.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BeskrivelseSafeValidator implements ConstraintValidator<BeskrivelseSafe, String> {

  private static final int MAX_LENGTH = 100;
  private static final Pattern CPR_PATTERN = Pattern.compile("\\d{10}");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    if (value.length() > MAX_LENGTH) {
      return false;
    }
    return !CPR_PATTERN.matcher(value).find();
  }
}
