package dk.ufst.opendebt.debtservice.validation;

import java.lang.annotation.*;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates that a beskrivelse field complies with PSRM GDPR requirements:
 *
 * <ul>
 *   <li>Maximum 100 characters
 *   <li>No CPR numbers (10 consecutive digits)
 *   <li>No PII of third parties
 * </ul>
 */
@Documented
@Constraint(validatedBy = BeskrivelseSafeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface BeskrivelseSafe {
  String message() default
      "Beskrivelse indeholder ugyldigt indhold (maks 100 tegn, ingen CPR-numre)";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
